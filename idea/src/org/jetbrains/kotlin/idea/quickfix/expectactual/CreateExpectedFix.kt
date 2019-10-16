/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showOkNoDialog
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.project.implementedModules
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.overrideImplement.makeActual
import org.jetbrains.kotlin.idea.core.overrideImplement.makeNotActual
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionsFactory
import org.jetbrains.kotlin.idea.quickfix.TypeAccessibilityChecker
import org.jetbrains.kotlin.idea.refactoring.getExpressionShortText
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class CreateExpectedFix<D : KtNamedDeclaration>(
    declaration: D,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module,
    generateIt: KtPsiFactory.(Project, TypeAccessibilityChecker, D) -> D?
) : AbstractCreateDeclarationFix<D>(declaration, commonModule, generateIt) {

    private val targetExpectedClassPointer = targetExpectedClass?.createSmartPointer()

    override fun getText() = "Create expected $elementType in common module ${module.name}"

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val targetExpectedClass = targetExpectedClassPointer?.element
        val expectedFile = targetExpectedClass?.containingKtFile ?: getOrCreateImplementationFile() ?: return
        doGenerate(project, editor, originalFile = file, targetFile = expectedFile, targetClass = targetExpectedClass)
    }

    override fun findExistingFileToCreateDeclaration(
        originalFile: KtFile,
        originalDeclaration: KtNamedDeclaration
    ): KtFile? {
        for (otherDeclaration in originalFile.declarations) {
            if (otherDeclaration === originalDeclaration) continue
            if (!otherDeclaration.hasActualModifier()) continue
            val expectedDeclaration = otherDeclaration.liftToExpected() ?: continue
            if (expectedDeclaration.module != module) continue
            return expectedDeclaration.containingKtFile
        }
        return null
    }

    companion object : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val d = DiagnosticFactory.cast(diagnostic, Errors.ACTUAL_WITHOUT_EXPECT)
            val declaration = d.psiElement as? KtNamedDeclaration ?: return emptyList()
            val compatibility = d.b
            // For function we allow it, because overloads are possible
            if (compatibility.isNotEmpty() && declaration !is KtFunction) return emptyList()

            val (actualDeclaration, expectedContainingClass) = findFirstActualWithExpectedClass(declaration)
            if (compatibility.isNotEmpty() && actualDeclaration !is KtFunction) return emptyList()

            // If there is already an expected class, we suggest only for its module,
            // otherwise we suggest for all relevant expected modules
            val expectedModules = expectedContainingClass?.module?.let { listOf(it) }
                ?: actualDeclaration.module?.implementedModules
                ?: return emptyList()
            return when (actualDeclaration) {
                is KtClassOrObject -> expectedModules.map { CreateExpectedClassFix(actualDeclaration, expectedContainingClass, it) }
                is KtProperty, is KtParameter, is KtFunction -> expectedModules.map {
                    CreateExpectedCallableMemberFix(
                        actualDeclaration as KtCallableDeclaration,
                        expectedContainingClass,
                        it
                    )
                }
                else -> emptyList()
            }
        }
    }
}

private tailrec fun findFirstActualWithExpectedClass(declaration: KtNamedDeclaration): Pair<KtNamedDeclaration, KtClassOrObject?> {
    val containingClass = declaration.containingClassOrObject
    val expectedContainingClass = containingClass?.liftToExpected() as? KtClassOrObject
    return if (containingClass != null && expectedContainingClass == null)
        findFirstActualWithExpectedClass(containingClass)
    else
        declaration to expectedContainingClass
}

class CreateExpectedClassFix(
    klass: KtClassOrObject,
    outerExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtClassOrObject>(klass, outerExpectedClass, commonModule, block@{ project, checker, element ->
    val originalElements = element.collectDeclarationsForAddActualModifier(withSelf = false).toList()
    val existingClasses = checker.findAndApplyExistingClasses(originalElements + klass)
    if (!checker.isCorrectAndHaveAccessibleModifiers(element, true)) return@block null

    val (members, declarationsWithNonExistentClasses) = originalElements.partition {
        checker.isCorrectAndHaveAccessibleModifiers(it)
    }

    if (!showUnknownTypeInDeclarationDialog(project, declarationsWithNonExistentClasses)) return@block null

    val membersForSelection = members.filter {
        !it.isAlwaysActual() && if (it is KtParameter) it.hasValOrVar() else true
    }

    val selectedElements = when {
        membersForSelection.all(KtDeclaration::hasActualModifier) -> membersForSelection
        ApplicationManager.getApplication().isUnitTestMode -> membersForSelection.filter(KtDeclaration::hasActualModifier)
        else -> {
            val prefix = klass.fqName?.asString()?.plus(".") ?: ""
            chooseMembers(project, membersForSelection, prefix) ?: return@block null
        }
    }.asSequence().plus(klass).plus(members.filter(KtNamedDeclaration::isAlwaysActual)).flatMap(KtNamedDeclaration::selected).toSet()

    val selectedClasses = checker.findAndApplyExistingClasses(selectedElements)
    val resultDeclarations = if (selectedClasses != existingClasses) {
        if (!checker.isCorrectAndHaveAccessibleModifiers(element, true)) return@block null

        val (resultDeclarations, withErrors) = selectedElements.partition {
            checker.isCorrectAndHaveAccessibleModifiers(it)
        }
        if (!showUnknownTypeInDeclarationDialog(project, withErrors)) return@block null
        resultDeclarations
    } else
        selectedElements

    project.executeWriteCommand("Repair actual members") {
        repairActualModifiers(originalElements + klass, resultDeclarations.toSet())
    }

    generateClassOrObject(project, true, element, checker)
})

private fun showUnknownTypeInDeclarationDialog(
    project: Project,
    declarationsWithNonExistentClasses: Collection<KtNamedDeclaration>
): Boolean {
    if (declarationsWithNonExistentClasses.isEmpty()) return true
    val message = escapeXml(
        declarationsWithNonExistentClasses.joinToString(
            prefix = "These declarations cannot be transformed:\n",
            separator = "\n",
            transform = ::getExpressionShortText
        )
    )

    TypeAccessibilityChecker.testLog?.append("$message\n")
    return ApplicationManager.getApplication().isUnitTestMode || showOkNoDialog(
        "Unknown types",
        message,
        project
    )
}

private fun KtDeclaration.canAddActualModifier() = when (this) {
    is KtEnumEntry, is KtClassInitializer -> false
    is KtParameter -> hasValOrVar()
    else -> true
}

/***
 * @return null if close without OK
 */
private fun chooseMembers(project: Project, collection: Collection<KtNamedDeclaration>, prefixToRemove: String): List<KtNamedDeclaration>? {
    val classMembers = collection.mapNotNull {
        it.resolveToDescriptorIfAny()?.let { descriptor -> Member(prefixToRemove, it, descriptor) }
    }
    val filter = if (collection.any(KtDeclaration::hasActualModifier)) {
        { declaration: KtDeclaration -> declaration.hasActualModifier() }
    } else {
        { true }
    }
    return MemberChooser(
        classMembers.toTypedArray(),
        true,
        true,
        project
    ).run {
        title = "Choose actual members"
        setCopyJavadocVisible(false)
        selectElements(classMembers.filter { filter((it.element as KtNamedDeclaration)) }.toTypedArray())
        show()
        if (!isOK) null else selectedElements?.map { it.element as KtNamedDeclaration }.orEmpty()
    }
}

private class Member(val prefix: String, element: KtElement, descriptor: DeclarationDescriptor) :
    DescriptorMemberChooserObject(element, descriptor) {
    override fun getText(): String {
        val text = super.getText()
        return if (descriptor is ClassDescriptor) text.removePrefix(prefix)
        else text
    }
}

fun KtClassOrObject.collectDeclarationsForAddActualModifier(withSelf: Boolean = true): Sequence<KtNamedDeclaration> {
    val thisSequence: Sequence<KtNamedDeclaration> = if (withSelf) sequenceOf(this) else emptySequence()
    val primaryConstructorSequence: Sequence<KtNamedDeclaration> = primaryConstructorParameters.asSequence() + primaryConstructor.let {
        if (it != null) sequenceOf(it) else emptySequence()
    }

    return thisSequence + primaryConstructorSequence + declarations.asSequence().flatMap {
        if (it.canAddActualModifier())
            when (it) {
                is KtClassOrObject -> it.collectDeclarationsForAddActualModifier()
                is KtNamedDeclaration -> sequenceOf(it)
                else -> emptySequence()
            }
        else
            emptySequence()
    }
}

private fun repairActualModifiers(
    originalElements: Collection<KtNamedDeclaration>,
    selectedElements: Collection<KtNamedDeclaration>
) {
    if (originalElements.size == selectedElements.size)
        for (original in originalElements) {
            original.makeActualWithParents()
        }
    else
        for (original in originalElements) {
            if (original in selectedElements)
                original.makeActualWithParents()
            else
                original.makeNotActual()
        }
}

private tailrec fun KtDeclaration.makeActualWithParents() {
    makeActual()
    containingClassOrObject?.takeUnless(KtDeclaration::hasActualModifier)?.makeActualWithParents()
}

private fun KtNamedDeclaration.selected(): Sequence<KtNamedDeclaration> {
    val additionalSequence = safeAs<KtParameter>()?.parent?.parent?.safeAs<KtPrimaryConstructor>()?.let {
        sequenceOf(it)
    } ?: emptySequence()
    return sequenceOf(this) + additionalSequence + containingClassOrObject?.selected().orEmpty()
}

class CreateExpectedCallableMemberFix(
    declaration: KtCallableDeclaration,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtNamedDeclaration>(declaration, targetExpectedClass, commonModule, block@{ project, checker, element ->
    if (!checker.isCorrectAndHaveAccessibleModifiers(element, true)) return@block null
    val descriptor = element.toDescriptor() as? CallableMemberDescriptor
    checker.existingTypeNames = targetExpectedClass?.getSuperNames()?.toSet().orEmpty()
    descriptor?.let {
        generateCallable(
            project,
            true,
            element,
            descriptor,
            targetExpectedClass,
            checker = checker
        )
    }
})
