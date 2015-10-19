/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler

import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.descriptorToKey
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

public abstract class KotlinClsFileBase(val provider: KotlinClassFileViewProvider) : KtFile(provider, true) {
    protected abstract val decompiledText: LockedClearableLazyValue<DecompiledText>

    public fun getDeclarationForDescriptor(descriptor: DeclarationDescriptor): KtDeclaration? {
        val original = descriptor.getOriginal()

        if (original is ValueParameterDescriptor) {
            val callable = original.getContainingDeclaration()
            val callableDeclaration = getDeclarationForDescriptor(callable) as? KtCallableDeclaration ?: return null
            return callableDeclaration.getValueParameters()[original.getIndex()]
        }

        if (original is ConstructorDescriptor && original.isPrimary()) {
            val classOrObject = getDeclarationForDescriptor(original.getContainingDeclaration()) as? KtClassOrObject
            return classOrObject?.getPrimaryConstructor() ?: classOrObject
        }

        val key = descriptorToKey(original)

        val range = decompiledText.get().renderedDescriptorsToRange[key]
        return if (range != null) {
            PsiTreeUtil.findElementOfClassAtRange(this, range.getStartOffset(), range.getEndOffset(), javaClass<KtDeclaration>())
        }
        else {
            null
        }
    }

    override fun getText(): String? {
        return decompiledText.get().text
    }

    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }

    @TestOnly
    fun getRenderedDescriptorsToRange(): Map<String, TextRange> {
        return decompiledText.get().renderedDescriptorsToRange
    }
}
