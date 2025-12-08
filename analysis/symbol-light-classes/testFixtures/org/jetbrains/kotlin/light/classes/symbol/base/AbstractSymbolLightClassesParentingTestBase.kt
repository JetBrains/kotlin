/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.ElementBase
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedCollectionClass
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.AssertionsService
import org.junit.Assume
import java.lang.reflect.Method
import java.nio.file.Path

open class AbstractSymbolLightClassesParentingTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_PARENTING_CHECK by directive(description = "Ignore the test")
    }

    override fun getRenderResult(
        ktFile: KtFile,
        ktFiles: List<KtFile>,
        testDataFile: Path,
        module: KtTestModule,
        project: Project,
    ): String {
        throw IllegalStateException("This test is not rendering light elements")
    }

    protected fun createLightElementsVisitor(directives: RegisteredDirectives, assertions: AssertionsService): JavaElementVisitor {
        Assume.assumeFalse("The test is not supported", Directives.IGNORE_PARENTING_CHECK in directives)

        // drop after KT-56882
        val ignoreDecompiledClasses = isTestAgainstCompiledCode
        return object : JavaElementVisitor() {
            private val declarationStack = ArrayDeque<PsiElement>()
            private val checkedClassesForIcons: MutableSet<Class<*>> = hashSetOf()

            private fun checkGetIconElementImplementation(element: PsiElement) {
                if (element !is ElementBase) return

                val javaClassToCheck = element.javaClass

                var method: Method? = null
                var currentClass: Class<*>? = javaClassToCheck
                while (currentClass != null && currentClass != Any::class.java) {
                    // This check helps to avoid search for already checked classes.
                    // If the super type of the class was already visited,
                    // it means that it is either having the correct override or the exception was already thrown
                    if (!checkedClassesForIcons.add(currentClass)) return

                    method = currentClass.declaredMethods.find {
                        it.name == "getElementIcon" && it.parameterCount == 1 && it.parameterTypes[0] == Integer.TYPE
                    }

                    if (method != null) break
                    currentClass = currentClass.superclass
                }

                // If method is completely absent in the hierarchy, fail fast to surface a regression
                assertions.assertNotNull(method) {
                    "getElementIcon(int) not found anywhere in hierarchy of ${javaClassToCheck.name}"
                }

                val owner = method!!.declaringClass
                assertions.assertNotEquals(ElementBase::class.java, owner) {
                    "${javaClassToCheck.name} relies on ElementBase.getElementIcon(int) instead of overriding it to `null`"
                }
            }

            private fun <T : PsiElement> checkParentAndVisitChildren(
                declaration: T,
                notCheckItself: Boolean = false,
                action: T.(visitor: JavaElementVisitor) -> Unit = {},
            ) {
                if (!notCheckItself) {
                    checkGetIconElementImplementation(declaration)
                    checkDeclarationParent(declaration)
                }

                if (declaration is PsiMember && declaration !is PsiTypeParameter) {
                    visitPsiMemberDeclaration(declaration)
                }

                declarationStack.addLast(declaration)
                try {
                    if (declaration is PsiModifierListOwner) {
                        declaration.modifierList?.accept(this)
                    }

                    if (declaration is PsiParameterListOwner) {
                        declaration.parameterList.accept(this)
                    }

                    if (declaration is PsiTypeParameterListOwner) {
                        declaration.typeParameterList?.accept(this)
                    }

                    declaration.action(this)
                } finally {
                    val removed = declarationStack.removeLast()
                    assertions.assertEquals(declaration, removed)
                }
            }

            private fun visitPsiMemberDeclaration(member: PsiMember) {
                val containingClass = member.containingClass
                val expectedClass = declarationStack.lastOrNull()
                if (expectedClass != null) {
                    assertions.assertEquals(expectedClass, containingClass)
                }

                val classToCheck: PsiClass = expectedClass?.let { it as PsiClass } ?: containingClass ?: return
                val memberToCheck = if (member is PsiEnumConstantInitializer) member.enumConstant else member
                val collection = when (memberToCheck) {
                    is PsiMethod -> classToCheck.methods
                    is PsiField -> classToCheck.fields
                    is PsiClass -> classToCheck.innerClasses
                    else -> error("Unexpected member: ${memberToCheck::class}\nElement: $memberToCheck")
                }

                assertions.assertTrue(memberToCheck in collection) {
                    "$memberToCheck is not found in:\n${collection.joinToString(separator = "\n")}\n"
                }
            }

            override fun visitModifierList(list: PsiModifierList) {
                checkParentAndVisitChildren(list, notCheckItself = ignoreDecompiledClasses) { visitor ->
                    annotations.forEach { it.accept(visitor) }
                }
            }

            override fun visitParameterList(list: PsiParameterList) {
                checkParentAndVisitChildren(list, notCheckItself = ignoreDecompiledClasses) { visitor ->
                    parameters.forEach { it.accept(visitor) }
                }
            }

            override fun visitTypeParameterList(list: PsiTypeParameterList) {
                checkParentAndVisitChildren(list, notCheckItself = ignoreDecompiledClasses) { visitor ->
                    typeParameters.forEach { it.accept(visitor) }
                }
            }

            override fun visitReferenceList(list: PsiReferenceList) {
                checkParentAndVisitChildren(list, notCheckItself = ignoreDecompiledClasses)
            }

            override fun visitClass(aClass: PsiClass) {
                checkParentAndVisitChildren(aClass) { visitor ->
                    annotations.forEach { it.accept(visitor) }

                    fields.forEach { it.accept(visitor) }
                    methods.forEach { it.accept(visitor) }
                    innerClasses.forEach { it.accept(visitor) }

                    implementsList?.accept(visitor)
                    extendsList?.accept(visitor)
                }
            }

            override fun visitField(field: PsiField) {
                checkParentAndVisitChildren(field) { visitor ->
                    annotations.forEach { it.accept(visitor) }
                }
            }

            override fun visitMethod(method: PsiMethod) {
                if (method is SyntheticElement || method is SymbolLightMethodForMappedCollectionClass) return

                checkParentAndVisitChildren(method) { visitor ->
                    annotations.forEach { it.accept(visitor) }
                }
            }

            override fun visitParameter(parameter: PsiParameter) {
                checkParentAndVisitChildren(parameter) { visitor ->
                    annotations.forEach { it.accept(visitor) }
                }
            }

            override fun visitTypeParameter(classParameter: PsiTypeParameter) {
                checkParentAndVisitChildren(classParameter) { visitor ->
                    annotations.forEach { it.accept(visitor) }
                }
            }

            override fun visitNameValuePair(pair: PsiNameValuePair) {
                checkParentAndVisitChildren(pair, notCheckItself = ignoreDecompiledClasses) {
                    value?.let(::checkAnnotationMemberValue)
                }
            }

            override fun visitAnnotationParameterList(list: PsiAnnotationParameterList) {
                checkParentAndVisitChildren(list, notCheckItself = ignoreDecompiledClasses) { visitor ->
                    attributes.forEach { it.accept(visitor) }
                }
            }

            private fun checkAnnotationMemberValue(memberValue: PsiAnnotationMemberValue) {
                checkParentAndVisitChildren(memberValue, notCheckItself = ignoreDecompiledClasses) {
                    if (this is PsiClassObjectAccessExpression) {
                        checkDeclarationParent(this.operand)
                    }

                    if (this is PsiArrayInitializerMemberValue) {
                        this.initializers.forEach(::checkAnnotationMemberValue)
                    }
                }
            }

            private fun checkDeclarationParent(declaration: PsiElement) {
                // NB: we deliberately put these retrievals before the bail-out below so that we can catch any potential exceptions.
                val context = declaration.context
                val parent = declaration.parent
                // NB: for a legitimate `null` parent case, e.g., an anonymous object as a return value of reified inline function,
                // it will not have an expected parent from the stack, and we can bail out early here.
                val expectedParent = declarationStack.lastOrNull() ?: return
                assertions.assertNotNull(context) {
                    "context should not be null for ${declaration::class} with text ${declaration.text}"
                }
                assertions.assertNotNull(parent) {
                    "Parent should not be null for ${declaration::class} with text ${declaration.text}"
                }
                assertions.assertEquals(expectedParent, parent) {
                    "Unexpected parent for ${declaration::class} with text ${declaration.text}"
                }

                assertions.assertNotEquals(declaration, parent) {
                    "Declaration and parent should not be the same for ${declaration::class.simpleName} with text ${declaration.text}"
                }

                assertions.assertNotEquals(parent, declaration) {
                    "Declaration and parent should not be the same for ${declaration::class.simpleName} with text ${declaration.text}"
                }
            }

            override fun visitAnnotation(annotation: PsiAnnotation) {
                val owner = annotation.owner
                assertions.assertNotNull(owner)

                val lastDeclaration = declarationStack.last()
                val psiModifierListOwner = when (lastDeclaration) {
                    is PsiTypeParameter -> null
                    is PsiModifierListOwner -> {
                        assertions.assertEquals(lastDeclaration.modifierList, owner)
                        lastDeclaration
                    }
                    else -> (lastDeclaration as PsiModifierList).parent as PsiModifierListOwner
                }

                if (!ignoreDecompiledClasses) {
                    when (psiModifierListOwner) {
                        is PsiClass, is PsiParameter -> assertions.assertTrue(owner is SymbolLightClassModifierList<*>)
                        is PsiField, is PsiMethod -> assertions.assertTrue(owner is SymbolLightMemberModifierList<*>)
                        null -> {}
                        else -> throw IllegalStateException("Unexpected annotation owner kind: ${lastDeclaration::class}")
                    }
                }

                val qualifiedName = annotation.qualifiedName!!
                // This is a workaround for IDEA-346155 bug
                if (!ignoreDecompiledClasses || owner !is PsiTypeParameter) {
                    assertions.assertTrue(owner!!.hasAnnotation(qualifiedName)) {
                        "$qualifiedName is not found in $owner"
                    }
                }

                val anno = owner.findAnnotation(qualifiedName)
                assertions.assertNotNull(anno) {
                    "$qualifiedName is not found in $owner"
                }

                assertions.assertTrue(annotation == anno || owner.annotations.count { it.qualifiedName == qualifiedName } > 1)

                assertions.assertTrue(owner.annotations.any { it == annotation }) {
                    "$annotation is not found in ${owner.annotations}"
                }

                checkParentAndVisitChildren(annotation, notCheckItself = true) { visitor ->
                    parameterList.accept(visitor)
                }
            }
        }
    }
}
