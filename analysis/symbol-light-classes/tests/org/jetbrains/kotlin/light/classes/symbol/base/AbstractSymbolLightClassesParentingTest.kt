/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import junit.framework.TestCase
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesParentingTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val fqName = LightClassTestCommon.fqNameInTestDataFile(testDataPath.toFile())

        val ktFile = ktFiles.first()
        val lightClass = findLightClass(fqName, ktFile.project)

        lightClass?.accept(createLightAnnotationVisitor(testServices.assertions))
    }

    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        throw IllegalStateException("This test is not rendering light elements")
    }

    private fun createLightAnnotationVisitor(assertions: AssertionsService) = object : JavaElementVisitor() {
        private val declarationStack = ArrayDeque<PsiModifierListOwner>()

        override fun visitClass(aClass: PsiClass?) {
            if (aClass == null) return
            checkDeclarationParent(aClass)
            declarationStack.addLast(aClass)

            aClass.annotations.forEach { it.accept(this) }

            aClass.fields.forEach { it.accept(this) }
            aClass.methods.forEach { it.accept(this) }
            aClass.innerClasses.forEach { it.accept(this) }

            aClass.typeParameterList?.typeParameters?.forEach { it.accept(this) }

            declarationStack.removeLast()
        }

        override fun visitField(field: PsiField?) {
            if (field == null) return
            checkDeclarationParent(field)
            declarationStack.addLast(field)

            field.annotations.forEach { it.accept(this) }

            field.type.annotations.forEach { it.accept(this) }

            declarationStack.removeLast()
        }

        override fun visitMethod(method: PsiMethod?) {
            if (method == null) return
            checkDeclarationParent(method)
            declarationStack.addLast(method)

            method.annotations.forEach { it.accept(this) }

            method.returnType?.annotations?.forEach { it.accept(this) }
            method.parameterList.parameters.forEach { it.accept(this) }

            method.typeParameterList?.typeParameters?.forEach { it.accept(this) }

            declarationStack.removeLast()
        }

        override fun visitParameter(parameter: PsiParameter?) {
            if (parameter == null) return
            checkDeclarationParent(parameter)
            declarationStack.addLast(parameter)

            parameter.annotations.forEach { it.accept(this) }

            declarationStack.removeLast()
        }

        override fun visitTypeParameter(classParameter: PsiTypeParameter?) {
            if (classParameter == null) return
            checkDeclarationParent(classParameter)
            declarationStack.addLast(classParameter)

            classParameter.annotations.forEach { it.accept(this) }

            declarationStack.removeLast()
        }

        private fun checkDeclarationParent(declaration: PsiElement) {
            val expectedParent = declarationStack.lastOrNull() ?: return
            val parent = when (declaration) {
                is PsiParameter -> {
                    val parameterList = declaration.parent as PsiParameterList
                    parameterList.parent
                }
                is PsiTypeParameter -> {
                    val parameterList = declaration.parent as PsiTypeParameterList
                    parameterList.parent
                }
                else -> declaration.parent
            }
            assertions.assertNotNull(parent) { "Parent should not be null for ${declaration::class} with text ${declaration.text} "}
            assertions.assertEquals(expectedParent, parent) {
                "Unexpected parent for ${declaration::class} with text ${declaration.text}"
            }
        }

        override fun visitAnnotation(annotation: PsiAnnotation?) {
            if (annotation == null) return

            val owner = annotation.owner
            TestCase.assertNotNull(owner)
            val lastDeclaration = declarationStack.last()
            TestCase.assertEquals(lastDeclaration.modifierList, owner)
            when (lastDeclaration) {
                is PsiClass,
                is PsiParameter ->
                    TestCase.assertTrue(owner is SymbolLightClassModifierList<*>)

                is PsiField,
                is PsiMethod ->
                    TestCase.assertTrue(owner is SymbolLightMemberModifierList<*>)

                else ->
                    throw IllegalStateException("Unexpected annotation owner kind: ${lastDeclaration::class.java}")
            }
        }
    }
}