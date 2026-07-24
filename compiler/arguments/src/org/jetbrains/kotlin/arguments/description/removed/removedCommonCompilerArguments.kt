/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description.removed

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.ReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.previous
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType

val removedCommonCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonCompilerArguments) {
    compilerArgument {
        name = "Xuse-k2"
        description = ReleaseDependent(
            "Compile using the K2 compiler pipeline.",
            KotlinReleaseVersion.v1_7_0..KotlinReleaseVersion.v2_4_0 to "Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet."
        )
        valueType = BooleanType.defaultFalse
        deprecatedMessage = "Compiler flag -Xuse-k2 is no more supported. " +
                "Compiler versions 2.0+ use K2 by default, unless the language version is set to 1.9 or earlier."

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

    compilerArgument {
        name = "Xcontext-receivers"
        description = "Enable experimental context receivers.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_20,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xsuppress-api-version-greater-than-language-version-error"
        val introducedVersion = KotlinReleaseVersion.v2_0_0
        val removedVersion = KotlinReleaseVersion.v2_5_0
        val commonDescriptionPart = "Suppress error about API version greater than language version."
        val commonDeprecationPart = "This is temporary solution (see KT-63712) intended to be used only for stdlib build."
        description = ReleaseDependent(
            commonDescriptionPart,
            introducedVersion..removedVersion.previous!! to "$commonDescriptionPart\nWarning: $commonDeprecationPart"
        )
        valueType = BooleanType.defaultFalse
        deprecatedMessage = commonDeprecationPart

        lifecycle(
            introducedVersion = introducedVersion,
            deprecatedVersion = introducedVersion, // It was deprecated upon introduction as it served only as a temporary workaround.
            removedVersion = removedVersion,
        )
    }
}
