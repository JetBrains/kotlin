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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.KotlinClsStubBuilder

public class JetClassFileDecompiler : ClassFileDecompilers.Full() {
    private val stubBuilder = KotlinClsStubBuilder()

    override fun accepts(file: VirtualFile) = isKotlinJvmCompiledFile(file)

    override fun getStubBuilder() = stubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean)
            = JetClassFileViewProvider(manager, file, physical, isKotlinInternalCompiledFile(file))
}
