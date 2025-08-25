/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.defaultTrue
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val actualCommonKlibBasedArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonKlibBasedArguments) {
    compilerArgument {
        name = "Xklib-relative-path-base"
        compilerName = "relativePathBases"
        description = "Provide a base path to compute the source's relative paths in klib (default is empty).".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xklib-normalize-absolute-path"
        compilerName = "normalizeAbsolutePath"
        description = "Normalize absolute paths in klibs.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xklib-enable-signature-clash-checks"
        compilerName = "enableSignatureClashChecks"
        description = "Enable signature uniqueness checks.".asReleaseDependent()
        valueType = BooleanType.defaultTrue

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xpartial-linkage"
        compilerName = "partialLinkageMode"
        description = "Use partial linkage mode.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{enable|disable}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xpartial-linkage-loglevel"
        compilerName = "partialLinkageLogLevel"
        description = "Define the compile-time log level for partial linkage.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{info|warning|error}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xklib-duplicated-unique-name-strategy"
        compilerName = "duplicatedUniqueNameStrategy"
        description = "Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{deny|allow-all-with-warning|allow-first-with-warning}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
        )
    }

    compilerArgument {
        name = "Xklib-ir-inliner"
        compilerName = "irInlinerBeforeKlibSerialization"
        description = "Enable experimental support to invoke IR Inliner before Klib serialization.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xklib-abi-version"
        compilerName = "customKlibAbiVersion"
        description = """Specify the custom ABI version to be written in KLIB. This option is intended only for tests.
Warning: This option does not affect KLIB ABI. Neither allows it making a KLIB backward-compatible with older ABI versions.
The only observable effect is that a custom ABI version is written to KLIB manifest file.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<version>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xklib-zip-file-accessor-cache-limit"
        description = "Maximum number of klibs that can be cached during compilation. Default is 64.".asReleaseDependent()
        valueType = IntType(
            defaultValue = 64.asReleaseDependent()
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0
        )
    }
}
