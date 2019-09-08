/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.TypeAccessibilityChecker
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

sealed class CreateActualFix<D : KtNamedDeclaration>(
    declaration: D,
    actualModule: Module,
    private val actualPlatform: TargetPlatform,
    generateIt: KtPsiFactory.(Project, TypeAccessibilityChecker, D) -> D?
) : AbstractCreateDeclarationFix<D>(declaration, actualModule, generateIt) {

    override fun getText() =
        "Create actual $elementType for module ${module.name} (${actualPlatform.singleOrNull()?.platformName ?: actualPlatform})"

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val actualFile = getOrCreateImplementationFile() ?: return
        doGenerate(project, editor, originalFile = file, targetFile = actualFile, targetClass = null)
    }

    override fun findExistingFileToCreateDeclaration(
        originalFile: KtFile,
        originalDeclaration: KtNamedDeclaration
    ): KtFile? {
        for (otherDeclaration in originalFile.declarations) {
            if (otherDeclaration === originalDeclaration) continue
            if (!otherDeclaration.hasExpectModifier()) continue
            val actualDeclaration = otherDeclaration.actualsForExpected(module).singleOrNull() ?: continue
            return actualDeclaration.containingKtFile
        }
        return null
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val d = DiagnosticFactory.cast(diagnostic, Errors.NO_ACTUAL_FOR_EXPECT)
            val declaration = d.psiElement as? KtNamedDeclaration ?: return null
            val compatibility = d.c
            // For function we allow it, because overloads are possible
            if (compatibility.isNotEmpty() && declaration !is KtFunction) return null
            val actualModuleDescriptor = d.b
            val actualModule = (actualModuleDescriptor.getCapability(ModuleInfo.Capability) as? ModuleSourceInfo)?.module ?: return null
            val actualPlatform = actualModuleDescriptor.platform ?: return null
            return when (declaration) {
                is KtClassOrObject -> CreateActualClassFix(declaration, actualModule, actualPlatform)
                is KtFunction, is KtProperty -> CreateActualCallableMemberFix(
                    declaration as KtCallableDeclaration,
                    actualModule,
                    actualPlatform
                )
                else -> null
            }
        }
    }
}

class CreateActualClassFix(
    klass: KtClassOrObject,
    actualModule: Module,
    actualPlatform: TargetPlatform
) : CreateActualFix<KtClassOrObject>(klass, actualModule, actualPlatform, { project, checker, element ->
    generateClassOrObject(project, false, element, checker = checker)
})

class CreateActualCallableMemberFix(
    declaration: KtCallableDeclaration,
    actualModule: Module,
    actualPlatform: TargetPlatform
) : CreateActualFix<KtCallableDeclaration>(declaration, actualModule, actualPlatform, { project, checker, element ->
    val descriptor = element.toDescriptor() as? CallableMemberDescriptor
    descriptor?.let { generateCallable(project, false, element, descriptor, checker = checker) }
})

