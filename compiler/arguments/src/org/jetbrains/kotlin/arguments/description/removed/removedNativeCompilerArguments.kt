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
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val removedNativeArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.nativeArguments) {
    compilerArgument {
        name = "Xg0"
        compilerName = "lightDebugDeprecated"
        description = ReleaseDependent(
            "Add light debug information.",
            KotlinReleaseVersion.v1_5_20..KotlinReleaseVersion.v2_4_0 to
                    "Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead."
        )
        valueType = BooleanType.defaultFalse
        deprecatedMessage = "Light debug information is enabled by default for Darwin platforms. " +
                "For other targets use '-Xadd-light-debug=enable' instead."

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_5_20,
            removedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }

    compilerArgument {
        name = "no-endorsed-libs"
        compilerName = "noendorsedlibs"
        description = ReleaseDependent(
            "Don't link endorsed libraries from the dist automatically.",
            KotlinReleaseVersion.v1_9_20..KotlinReleaseVersion.v2_4_0 to
                    "Don't link endorsed libraries from the dist automatically. This option has been deprecated, as the dist no longer has any endorsed libraries."
        )
        valueType = BooleanType.defaultFalse
        deprecatedMessage = "The dist no longer has any endorsed libraries."

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_9_20,
            removedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }

    compilerArgument {
        name = "library-version"
        shortName = "lv"
        description = ReleaseDependent(
            "The library version.",
            KotlinReleaseVersion.v2_0_20..KotlinReleaseVersion.v2_4_0 to
                    "The library version.\nNote: This option is deprecated and will be removed in one of the future releases."
        )
        valueType = StringType.defaultNull
        valueDescription = "<version>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v2_0_20,
            removedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }

    compilerArgument {
        name = "Xworker-exception-handling"
        description = "Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<mode>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
            deprecatedVersion = KotlinReleaseVersion.v2_4_20,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }
}
