/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule

/**
 * [KotlinPlatformSettings] allow the Analysis API platform to control the behavior of the Analysis API engine.
 */
@KaPlatformInterface
public interface KotlinPlatformSettings : KotlinPlatformComponent {
    /**
     * @see KotlinDeserializedDeclarationsOrigin
     */
    public val deserializedDeclarationsOrigin: KotlinDeserializedDeclarationsOrigin

    /**
     * Whether analysis of use-site [KaLibraryModule]s is allowed by the platform. When this is `false`, attempts to analyze a
     * [KaLibraryModule] will result in an exception.
     *
     * See KT-76042 for more information.
     */
    public val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = true

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KotlinPlatformSettings = project.service()
    }
}

/**
 * This [setting][KotlinPlatformSettings] controls where [declarations][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider]
 * provided by the platform come from.
 *
 * The origin directly affects whether declaration providers have to provide library entities in addition to source entities, which is the
 * case for the [STUBS] origin.
 *
 * Internally, the Analysis API engine has to use different implementations of symbol providers for [BINARIES] and [STUBS].
 */
@KaPlatformInterface
public enum class KotlinDeserializedDeclarationsOrigin {
    /**
     * Library content is deserialized from `.class` files, KLIBs, and metadata.
     *
     * Kotlin FIR declarations deserialized from binaries don't have associated PSI elements.
     */
    BINARIES,

    /**
     * Library content is pre-indexed to [stubs](https://plugins.jetbrains.com/docs/intellij/stub-indexes.html), which are then provided by
     * [declaration providers][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider].
     *
     * This mode is used by the IntelliJ K2 plugin because libraries are already indexed in a stub format, and we can avoid additionally
     * loading binaries.
     */
    STUBS,
}
