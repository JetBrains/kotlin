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

import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.compiled.ClsFileImpl

public class JetDecompilerForWrongAbiVersion : ClassFileDecompilers.Light() {
    override fun accepts(file: VirtualFile) = isKotlinCompiledFileWithIncompatibleAbiVersion(file)

    override fun getText(file: VirtualFile) = "$INCOMPATIBLE_ABI_VERSION_COMMENT\n${ClsFileImpl.decompile(file)}"
}

val INCOMPATIBLE_ABI_VERSION_COMMENT = "  // This Kotlin file has an incompatible ABI version and is displayed as Java class"
