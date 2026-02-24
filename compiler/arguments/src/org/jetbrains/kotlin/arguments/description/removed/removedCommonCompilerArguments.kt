/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description.removed

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType

val removedCommonCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonCompilerArguments) {
    compilerArgument {
        name = "Xuse-k2"
        description =
            "Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_0,
            removedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }
}