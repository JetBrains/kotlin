/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.ICBuildSystem.*

/**
 * This is internal API. From incremental compilation's point of view,
 * build system provides a set of constraints and guarantees.
 * These guarantees may vary, so it makes sense to account for them
 * in the general IC algorithm.
 */
enum class ICBuildSystem {
    UNKNOWN,
    JPS,
    GRADLE,
    MAVEN,
    ANT,
    BAZEL,
    CLI,
}

/**
 * This is internal API.
 *
 * In the future, this might have properties such as `normalizesSourcePaths`,
 * `tracksChangedFiles`, etc.
 *
 * Depending on tool's configuration and version, it might provide different guarantees.
 * So not all properties would be deducible based on `buildSystem` alone.
 */
class BuildSystemSpecification(
    private val buildSystem: ICBuildSystem
) {
    //TODO write IC-friendly code - do we add extensions to separate files? would it help?
    val methodToEnableICFallback: String?
        get() = when (buildSystem) {
            GRADLE -> "Add foo.bar.tmp=true to gradle.properties"
            MAVEN -> "Add property <kotlin.compiler.incremental.fallback>true</kotlin.compiler.incremental.fallback> to pom.xml"
            else -> null
        }?.let {
            """
                If you see this message too often, you can enable the automatic fallback to full compilation.
                
                $it
                
                Please let us know in KT-???. Based on user feedback, we could enable fallback by default in the stable release.
            """.trimIndent()
        }

    companion object {
        val unknownBuildSystem = BuildSystemSpecification(UNKNOWN)
    }
}
