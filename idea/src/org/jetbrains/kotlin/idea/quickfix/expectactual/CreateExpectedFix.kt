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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.project.implementedModules
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionsFactory
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier

sealed class CreateExpectedFix<out D : KtNamedDeclaration>(
    declaration: D,
    targetExpectedClass: KtClassOrObject?,
    private val commonModule: Module,
    private val generateIt: KtPsiFactory.(Project, D) -> D?
) : KotlinQuickFixAction<D>(declaration) {

    private val targetExpectedClassPointer = targetExpectedClass?.createSmartPointer()

    override fun getFamilyName() = text

    protected val elementType: String = element.getTypeDescription()

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
    generateClassOrObject(project, true, element, listOfNotNull(outerExpectedClass))
})

class CreateExpectedPropertyFix(
    property: KtNamedDeclaration,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtNamedDeclaration>(property, targetExpectedClass, commonModule, { project, element ->
    val descriptor = element.toDescriptor() as? PropertyDescriptor
    descriptor?.let { generateProperty(project, true, element, descriptor, targetExpectedClass) }
})

class CreateExpectedFunctionFix(
    function: KtFunction,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtFunction>(function, targetExpectedClass, commonModule, { project, element ->
    val descriptor = element.toDescriptor() as? FunctionDescriptor
    descriptor?.let { generateFunction(project, true, element, descriptor, targetExpectedClass) }
})

