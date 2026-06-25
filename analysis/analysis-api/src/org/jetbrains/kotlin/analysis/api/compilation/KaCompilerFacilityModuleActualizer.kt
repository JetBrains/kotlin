/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSpi
import org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * Actualizer for common source modules.
 *
 * The Kotlin compiler cannot directly compile classes from common modules, as it needs dependencies and language settings from the target
 * platform. Such as, even if the compiled class only uses 'kotlin-stdlib', the JVM compiler still needs the library bytecode to understand
 * JVM facade names and to be able to inline functions (the JVM inliner uses Java bytecode instead of the serialized IR).
 *
 * [compile] attempts to find the platform module with an appropriate target by itself and substitutes it instead of the original
 * common module – that way, it can pass all the required information to the compiler. However, there might be multiple platform modules
 * (e.g., Android and JVM); in that case, the facility chooses the first matching one. [KaCompilerFacilityModuleActualizer] is a way to
 * override the default behavior by offering a closer match – e.g., a module with an Android target.
 */
@KaExperimentalApi
@KaSpi
public fun interface KaCompilerFacilityModuleActualizer {
    /**
     * Actualizes the [module] with the common multiplatform target.
     * Returns an actual counterpart of [module], target of which matches the [target], or `null` if such a module does not exist.
     */
    @KaSpiExtensionPoint
    public fun actualize(module: KaModule, target: KaCompilationTarget): KaModule?
}
