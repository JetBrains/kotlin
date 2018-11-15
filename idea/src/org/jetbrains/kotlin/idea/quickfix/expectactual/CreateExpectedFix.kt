/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.project.implementedDescriptors
import org.jetbrains.kotlin.idea.caches.project.implementedModules
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.overrideImplement.*
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionsFactory
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.hasDeclarationOf
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.KotlinType

sealed class CreateExpectedFix<out D : KtNamedDeclaration>(
    declaration: D,
    targetExpectedClass: KtClassOrObject?,
    private val commonModule: Module,
    private val generateIt: KtPsiFactory.(Project, D) -> D?
) : KotlinQuickFixAction<D>(declaration) {

    private val targetExpectedClassPointer = targetExpectedClass?.createSmartPointer()

    override fun getFamilyName() = text

    protected abstract val elementType: String

    override fun getText() = "Create expected $elementType in common module ${commonModule.name}"

    override fun startInWriteAction() = false

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(project)

        val targetExpectedClass = targetExpectedClassPointer?.element
        val expectedFile = targetExpectedClass?.containingKtFile ?: getOrCreateImplementationFile() ?: return
        DumbService.getInstance(project).runWhenSmart {
            val generated = try {
                factory.generateIt(project, element) ?: return@runWhenSmart
            } catch (e: KotlinTypeInaccessibleException) {
                if (editor != null) {
                    showErrorHint(project, editor, "Cannot generate expected $elementType: " + e.message, e.message)
                }
                return@runWhenSmart
            }

            project.executeWriteCommand("Create expected declaration") {
                if (expectedFile.packageDirective?.fqName != file.packageDirective?.fqName &&
                    expectedFile.declarations.isEmpty()
                ) {
                    val packageDirective = file.packageDirective
                    packageDirective?.let {
                        val oldPackageDirective = expectedFile.packageDirective
                        val newPackageDirective = factory.createPackageDirective(it.fqName)
                        if (oldPackageDirective != null) {
                            oldPackageDirective.replace(newPackageDirective)
                        } else {
                            expectedFile.add(newPackageDirective)
                        }
                    }
                }
                val expectedDeclaration = when {
                    targetExpectedClass != null -> targetExpectedClass.addDeclaration(generated as KtNamedDeclaration)
                    else -> expectedFile.add(generated) as KtElement
                }
                val reformatted = CodeStyleManager.getInstance(project).reformat(expectedDeclaration)
                val shortened = ShortenReferences.DEFAULT.process(reformatted as KtElement)
                EditorHelper.openInEditor(shortened)?.caretModel?.moveToOffset(shortened.textRange.startOffset)
            }
        }
    }

    private fun getOrCreateImplementationFile(): KtFile? {
        val declaration = element as? KtNamedDeclaration ?: return null
        val parent = declaration.parent
        if (parent is KtFile) {
            for (otherDeclaration in parent.declarations) {
                if (otherDeclaration === declaration) continue
                if (!otherDeclaration.hasActualModifier()) continue
                val expectedDeclaration = otherDeclaration.liftToExpected() ?: continue
                return expectedDeclaration.containingKtFile
            }
        }
        return createFileForDeclaration(commonModule, declaration)
    }

    companion object : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val d = DiagnosticFactory.cast(diagnostic, Errors.ACTUAL_WITHOUT_EXPECT)
            val declaration = d.psiElement as? KtNamedDeclaration ?: return emptyList()
            val compatibility = d.b
            // For function we allow it, because overloads are possible
            if (compatibility.isNotEmpty() && declaration !is KtFunction) return emptyList()

            val containingClass = declaration.containingClassOrObject
            val expectedContainingClass = containingClass?.liftToExpected() as? KtClassOrObject
            if (containingClass != null && expectedContainingClass == null) {
                // In this case fix should be invoked on containingClass
                return emptyList()
            }
            // If there is already an expected class, we suggest only for its module,
            // otherwise we suggest for all relevant expected modules
            val expectedModules = expectedContainingClass?.module?.let { listOf(it) }
                ?: declaration.module?.implementedModules
                ?: return emptyList()
            return when (declaration) {
                is KtClassOrObject -> expectedModules.map { CreateExpectedClassFix(declaration, expectedContainingClass, it) }
                is KtFunction -> expectedModules.map { CreateExpectedFunctionFix(declaration, expectedContainingClass, it) }
                is KtProperty, is KtParameter -> expectedModules.map { CreateExpectedPropertyFix(declaration, expectedContainingClass, it) }
                else -> emptyList()
            }
        }
    }
}

class CreateExpectedClassFix(
    klass: KtClassOrObject,
    outerExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtClassOrObject>(klass, outerExpectedClass, commonModule, { project, element ->
    generateClassOrObjectByActualClass(project, element, listOfNotNull(outerExpectedClass))
}) {

    override val elementType = element.getTypeDescription()
}

class CreateExpectedPropertyFix(
    property: KtNamedDeclaration,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtNamedDeclaration>(property, targetExpectedClass, commonModule, { project, element ->
    val descriptor = element.toDescriptor() as? PropertyDescriptor
    descriptor?.let { generateProperty(project, element, descriptor, targetExpectedClass, emptyList()) }
}) {

    override val elementType = "property"
}

class CreateExpectedFunctionFix(
    function: KtFunction,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtFunction>(function, targetExpectedClass, commonModule, { project, element ->
    val descriptor = element.toDescriptor() as? FunctionDescriptor
    descriptor?.let { generateFunction(project, element, descriptor, targetExpectedClass, emptyList()) }
}) {

    override val elementType = "function"
}

private fun KtPsiFactory.generateClassOrObjectByActualClass(
    project: Project,
    actualClass: KtClassOrObject,
    outerExpectedClasses: List<KtClassOrObject>
): KtClassOrObject {
    val expectedClass = createClassCopyByText(actualClass)
    expectedClass.declarations.forEach {
        when (it) {
            is KtClassOrObject -> it.delete()
            is KtCallableDeclaration -> it.delete()
            is KtAnonymousInitializer -> it.delete()
        }
    }
    expectedClass.primaryConstructor?.delete()

    val context = actualClass.analyzeWithContent()
    expectedClass.superTypeListEntries.zip(actualClass.superTypeListEntries).forEach { (expectedEntry, actualEntry) ->
        if (expectedEntry !is KtSuperTypeCallEntry) return@forEach
        val superType = context[BindingContext.TYPE, actualEntry.typeReference]
        val superClassDescriptor = superType?.constructor?.declarationDescriptor as? ClassDescriptor ?: return@forEach
        if (superClassDescriptor.kind == ClassKind.CLASS || superClassDescriptor.kind == ClassKind.ENUM_CLASS) {
            expectedEntry.replace(createSuperTypeEntry(expectedEntry.typeReference!!.text))
        }
    }
    if (outerExpectedClasses.isEmpty()) {
        expectedClass.addModifier(KtTokens.EXPECT_KEYWORD)
    } else {
        expectedClass.makeNotActual()
    }
    expectedClass.removeModifier(KtTokens.DATA_KEYWORD)

    declLoop@ for (actualDeclaration in actualClass.declarations) {
        val descriptor = actualDeclaration.toDescriptor() ?: continue
        if (!actualDeclaration.isEffectivelyActual()) continue@declLoop
        val expectedDeclaration: KtDeclaration = when (actualDeclaration) {
            is KtClassOrObject -> {
                generateClassOrObjectByActualClass(project, actualDeclaration, outerExpectedClasses + expectedClass)
            }
            is KtCallableDeclaration -> {
                when (actualDeclaration) {
                    is KtFunction ->
                        generateFunction(project, actualDeclaration, descriptor as FunctionDescriptor, expectedClass, outerExpectedClasses)
                    is KtProperty ->
                        generateProperty(project, actualDeclaration, descriptor as PropertyDescriptor, expectedClass, outerExpectedClasses)
                    else -> continue@declLoop
                }
            }
            else -> continue@declLoop
        }
        expectedClass.addDeclaration(expectedDeclaration)
    }
    if (!actualClass.isAnnotation()) {
        for (actualProperty in actualClass.primaryConstructorParameters) {
            if (!actualProperty.hasValOrVar() || !actualProperty.hasActualModifier()) continue
            val descriptor = actualProperty.toDescriptor() as? PropertyDescriptor ?: continue
            val expectedProperty =
                generateProperty(project, actualProperty, descriptor, expectedClass, outerExpectedClasses)
            expectedClass.addDeclaration(expectedProperty)
        }
    }
    val actualPrimaryConstructor = actualClass.primaryConstructor
    if (expectedClass is KtClass && actualPrimaryConstructor != null) {
        val descriptor = actualPrimaryConstructor.toDescriptor()
        if (descriptor is FunctionDescriptor) {
            val expectedPrimaryConstructor =
                generateFunction(project, actualPrimaryConstructor, descriptor, expectedClass, outerExpectedClasses)
            expectedClass.createPrimaryConstructorIfAbsent().replace(expectedPrimaryConstructor)
        }
    }

    return expectedClass
}

private fun generateFunction(
    project: Project,
    actualFunction: KtFunction,
    descriptor: FunctionDescriptor,
    targetClass: KtClassOrObject?,
    outerExpectedClasses: List<KtClassOrObject>
): KtFunction {
    val accessibleClasses = outerExpectedClasses + listOfNotNull(targetClass)
    descriptor.checkTypeParameterBoundsAccessibility(accessibleClasses)
    descriptor.returnType?.checkAccessibility(accessibleClasses)
    descriptor.valueParameters.forEach {
        it.type.checkAccessibility(accessibleClasses)
    }
    val memberChooserObject = OverrideMemberChooserObject.create(
        actualFunction, descriptor, descriptor,
        OverrideMemberChooserObject.BodyType.NO_BODY
    )
    return if (targetClass != null) {
        memberChooserObject.generateExpectMember(targetClass = targetClass, copyDoc = true)
    } else {
        memberChooserObject.generateTopLevelExpect(project = project, copyDoc = true)
    } as KtFunction
}

private fun generateProperty(
    project: Project,
    actualProperty: KtNamedDeclaration,
    descriptor: PropertyDescriptor,
    targetClass: KtClassOrObject?,
    outerExpectedClasses: List<KtClassOrObject>
): KtProperty {
    val accessibleClasses = outerExpectedClasses + listOfNotNull(targetClass)
    descriptor.checkTypeParameterBoundsAccessibility(accessibleClasses)
    descriptor.type.checkAccessibility(accessibleClasses)
    val memberChooserObject = OverrideMemberChooserObject.create(
        actualProperty, descriptor, descriptor,
        OverrideMemberChooserObject.BodyType.NO_BODY
    )
    return if (targetClass != null) {
        memberChooserObject.generateExpectMember(targetClass = targetClass, copyDoc = true)
    } else {
        memberChooserObject.generateTopLevelExpect(project = project, copyDoc = true)
    } as KtProperty
}

private fun CallableMemberDescriptor.checkTypeParameterBoundsAccessibility(accessibleClasses: List<KtClassOrObject>) {
    typeParameters.forEach { typeParameter ->
        typeParameter.upperBounds.forEach { upperBound ->
            upperBound.checkAccessibility(accessibleClasses)
        }
    }
}

private fun KotlinType.checkAccessibility(accessibleClasses: List<KtClassOrObject>) {
    for (argument in arguments) {
        if (argument.isStarProjection) continue
        argument.type.checkAccessibility(accessibleClasses)
    }
    val classifierDescriptor = constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters ?: return
    val moduleDescriptor = classifierDescriptor.module
    if (moduleDescriptor.getMultiTargetPlatform() == MultiTargetPlatform.Common) {
        // Common classes are Ok
        return
    }
    val implementedDescriptors = moduleDescriptor.implementedDescriptors
    if (implementedDescriptors.isEmpty()) {
        // This happens now if we are not in sources, in this case yet we cannot answer question about accessibility
        // Very rude check about JDK classes
        if (!classifierDescriptor.fqNameSafe.toString().startsWith("java.")) return
    }
    if (implementedDescriptors.any { it.hasDeclarationOf(classifierDescriptor) }) {
        // Platform classes with expected class are also Ok
        return
    }
    accessibleClasses.forEach {
        if (classifierDescriptor.name == it.nameAsName) return
    }
    if (this is AbbreviatedType) {
        // For type aliases without expected class, check expansions instead
        expandedType.checkAccessibility(accessibleClasses)
    } else {
        throw KotlinTypeInaccessibleException(this)
    }
}

class KotlinTypeInaccessibleException(val type: KotlinType) : Exception() {
    override val message: String
        get() = "Type ${type.getJetTypeFqName(true)} is not accessible from common code"
}