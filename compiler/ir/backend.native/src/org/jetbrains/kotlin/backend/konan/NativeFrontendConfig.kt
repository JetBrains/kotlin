/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibsInProductionPipeline
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.konan.config.konanTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.LibraryOrder

/**
 * The implementation of [NativeKlibCompilationConfig] for the first compilation stage.
 */
class NativeFrontendConfig(
    project: Project,
    configuration: CompilerConfiguration,
) : NativeKlibCompilationConfig(project, configuration) {
    override val target: KonanTarget = HostManager().targetManager(configuration.konanTarget).target

    override val resolvedLibraries: KotlinLibraryResolveResult = object : KotlinLibraryResolveResult {
        private val libraries: List<KotlinLibrary> = loadNativeKlibsInProductionPipeline(configuration).all

        override fun filterRoots(predicate: (KotlinResolvedLibrary) -> Boolean): KotlinLibraryResolveResult {
            TODO("Not yet implemented")
        }

        override fun getFullResolvedList(order: LibraryOrder?): List<KotlinResolvedLibrary> {
            TODO("Not yet implemented")
        }

        override fun forEach(action: (KotlinLibrary, PackageAccessHandler) -> Unit) {
            TODO("Not yet implemented")
        }
    }

    override val moduleId: String
        get() = configuration.moduleName!!
}
