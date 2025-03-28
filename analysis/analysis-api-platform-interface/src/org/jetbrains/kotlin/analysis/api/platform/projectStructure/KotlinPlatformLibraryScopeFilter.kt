/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

public enum class KotlinPlatformLibraryScopeFilter(private val filter: (VirtualFile) -> Boolean) {
    /**
     * An empty filter (matches all files).
     */
    ANY({ true }),

    /**
     * A filter for Kotlin class files (used in JVM code analysis).
     */
    JVM(
        fun(file: VirtualFile): Boolean {
            val extension = file.extension
            return extension == JavaClassFileType.INSTANCE.defaultExtension
                    || extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
        }
    ),

    /**
     * A filter for Kotlin metadata files (used in Common code analysis).
     */
    METADATA(
        fun(file: VirtualFile): Boolean {
            val extension = file.extension
            return extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION ||
                    extension == METADATA_FILE_EXTENSION ||
                    // klib metadata symbol provider
                    extension == KLIB_METADATA_FILE_EXTENSION
        }
    ),

    /**
     * A filter for Kotlin KLib files (used in non-JVM and non-Common code, including Native, JavaScript and WASM modules).
     */
    KLIB(
        fun(file: VirtualFile): Boolean {
            return file.extension == KLIB_METADATA_FILE_EXTENSION
        }
    );

    public fun matches(file: VirtualFile): Boolean = filter(file)

    public companion object {
        public fun forPlatform(platform: TargetPlatform): KotlinPlatformLibraryScopeFilter {
            return when {
                platform.isJvm() -> JVM
                platform.isCommon() -> METADATA
                else -> KLIB
            }
        }
    }
}