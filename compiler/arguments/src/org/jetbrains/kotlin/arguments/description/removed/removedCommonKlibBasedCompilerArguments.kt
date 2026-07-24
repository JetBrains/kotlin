/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description.removed

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerPhase
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType

val removedCommonKlibBasedCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonKlibBasedArguments) {
    compilerArgument {
        name = "Xklib-normalize-absolute-path"
        compilerName = "normalizeAbsolutePath"
        description = "Normalize absolute paths in klibs.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
            deprecatedVersion = KotlinReleaseVersion.v2_4_20,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
        restrictedToCompilerPhase = KotlinCompilerPhase.KLIB_COMPILATION
    }
}
