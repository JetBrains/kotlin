/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent

/**
 * Provides the `.kotlin_module` files of binary libraries, from which the engine reads package parts and optional annotation classes.
 *
 * A platform must implement the provider if it uses
 * [KotlinDeserializedDeclarationsOrigin.BINARIES][org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES],
 * as top-level callables of JVM and metadata libraries cannot be found without package parts.
 */
@KaPlatformInterface
public interface KotlinModuleMappingFileProvider : KotlinOptionalPlatformComponent {
    /**
     * Returns the `.kotlin_module` files from the `META-INF` directories of the binary roots in [scope].
     */
    public fun findModuleMappingFiles(scope: GlobalSearchScope): Collection<VirtualFile>

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KotlinModuleMappingFileProvider? = project.serviceOrNull()
    }
}
