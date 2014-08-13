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

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.resolve.kotlin.KotlinBinaryClassCache
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import com.intellij.psi.ClassFileViewProvider
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass
import com.intellij.ide.highlighter.JavaClassFileType

public fun isKotlinCompiledFile(file: VirtualFile): Boolean {
    if (isKotlinCompiledFileWithIncompatibleAbiVersion(file)) {
        return false
    }
    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file)?.getClassHeader()
    return header != null && header.syntheticClassKind != KotlinSyntheticClass.Kind.TRAIT_IMPL
}

public fun isKotlinCompiledFileWithIncompatibleAbiVersion(file: VirtualFile): Boolean {
    if (file.getExtension() != JavaClassFileType.INSTANCE!!.getDefaultExtension()) {
        return false
    }
    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file)?.getClassHeader()
    return header?.kind == KotlinClassHeader.Kind.INCOMPATIBLE_ABI_VERSION
}

public fun isKotlinInternalCompiledFile(file: VirtualFile): Boolean {
    if (!isKotlinCompiledFile(file)) {
        return false
    }
    if (ClassFileViewProvider.isInnerClass(file)) {
        return true
    }
    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file)?.getClassHeader()
    return header?.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS
}
