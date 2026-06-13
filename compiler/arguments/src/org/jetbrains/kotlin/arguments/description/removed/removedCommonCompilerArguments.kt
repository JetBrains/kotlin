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
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

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

    compilerArgument {
        name = "Xverify-ir-visibility"
        description =
            "Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
            removedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }

    compilerArgument {
        name = "Xverify-ir-nested-offsets"
        description =
            "Check that offsets of nested IR elements conform to offsets of their containers. Only has effect if '-Xverify-ir' is not 'none'.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_20,
            removedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }
}
