/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.facet.implementingDescriptors
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.diagnostics.SimpleDiagnostics

class PlatformExpectedAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val declaration = element as? KtDeclaration ?: return
        if (!isExpectedDeclaration(declaration)) return

        if (TargetPlatformDetector.getPlatform(declaration.containingKtFile) !is TargetPlatform.Common) return

        val implementingModules = declaration.findModuleDescriptor().implementingDescriptors
        if (implementingModules.isEmpty()) return

        val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return
        if (!descriptor.isExpect) return

        val trace = BindingTraceContext()
        for (module in implementingModules) {
            ExpectedActualDeclarationChecker.checkExpectedDeclarationHasActual(declaration, descriptor, trace, module)
        }

        val suppressionCache = KotlinCacheService.getInstance(declaration.project).getSuppressionCache()
        val filteredList = trace.bindingContext.diagnostics.filter { diagnostic ->
            !suppressionCache.isSuppressed(declaration, diagnostic.factory.name, diagnostic.severity)
        }
        if (filteredList.isEmpty()) return

        KotlinPsiChecker().annotateElement(declaration, holder, SimpleDiagnostics(filteredList))
    }

    private fun isExpectedDeclaration(declaration: KtDeclaration): Boolean {
        return declaration.hasExpectModifier() ||
               declaration is KtClassOrObject && KtPsiUtil.getOutermostClassOrObject(declaration)?.hasExpectModifier() == true
    }
}
