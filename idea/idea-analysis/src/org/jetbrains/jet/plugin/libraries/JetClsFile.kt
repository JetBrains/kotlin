/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.libraries

import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.psi.JetDeclaration
import kotlin.properties.Delegates

public class JetClsFile(val provider: JetClassFileViewProvider) : ClsFileImpl(provider) {

    private val decompiledFile by Delegates.blockingLazy(this) {
        JetDummyClassFileViewProvider.createJetFile(provider.getManager(), getVirtualFile(), provider.decompiledText.text)
    }

    override fun getDecompiledPsiFile() = decompiledFile

    public fun getDeclarationForDescriptor(descriptor: DeclarationDescriptor): JetDeclaration? {
        val key = descriptorToKey(descriptor)
        val range = provider.decompiledText.renderedDescriptorsToRange[key]
        if (range == null) {
            return null
        }
        return PsiTreeUtil.findElementOfClassAtRange(decompiledFile, range.getStartOffset(), range.getEndOffset(), javaClass<JetDeclaration>())
    }

    TestOnly
    fun getRenderedDescriptorsToRange(): Map<String, TextRange> {
        return provider.decompiledText.renderedDescriptorsToRange
    }
}
