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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class KotlinTypeDeclarationProvider : TypeDeclarationProvider {
    override fun getSymbolTypeDeclarations(symbol: PsiElement): Array<PsiElement>? {
        if (symbol !is KtElement || symbol.getContainingFile() !is KtFile) return emptyArray()

        val bindingContext = symbol.analyze()
        val callableDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, symbol)
        if (callableDescriptor !is CallableDescriptor) return emptyArray()

        val type = callableDescriptor.returnType ?: return emptyArray()

        val classifierDescriptor = type.constructor.declarationDescriptor ?: return emptyArray()
        return DescriptorToSourceUtilsIde.getAllDeclarations(symbol.project, classifierDescriptor).toTypedArray()
    }
}
