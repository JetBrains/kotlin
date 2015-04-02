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

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader

/**
 * Checks if this file is a compiled Kotlin class file (not necessarily ABI-compatible with the current plugin)
 */
public fun isKotlinCompiledFile(file: VirtualFile): Boolean {
    if (file.getExtension() != JavaClassFileType.INSTANCE!!.getDefaultExtension()) {
        return false
    }

    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file)?.getClassHeader()
    return header != null &&
           header.syntheticClassKind != KotlinSyntheticClass.Kind.TRAIT_IMPL &&
           header.syntheticClassKind != KotlinSyntheticClass.Kind.LOCAL_TRAIT_IMPL
}

/**
 * Checks if this file is a compiled Kotlin class file ABI-compatible with the current plugin
 */
public fun isKotlinWithCompatibleAbiVersion(file: VirtualFile): Boolean {
    if (!isKotlinCompiledFile(file)) return false

    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file)?.getClassHeader()
    return header != null && header.isCompatibleAbiVersion
}

/**
 * Checks if this file is a compiled "internal" Kotlin class, i.e. a Kotlin class (not necessarily ABI-compatible with the current plugin)
 * which should NOT be decompiled (and, as a result, shown under the library in the Project view, be searchable via Find class, etc.)
 */
public fun isKotlinInternalCompiledFile(file: VirtualFile): Boolean {
    if (!isKotlinCompiledFile(file)) {
        return false
    }

    if (ClassFileViewProvider.isInnerClass(file)) {
        return true
    }
    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file)?.getClassHeader() ?: return false
    return header.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS ||
           (header.kind == KotlinClassHeader.Kind.CLASS && header.classKind != null && header.classKind != KotlinClass.Kind.CLASS)
}
