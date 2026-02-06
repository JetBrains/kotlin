/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.defaultTrue
import org.jetbrains.kotlin.arguments.dsl.types.*

val actualCommonKlibBasedArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonKlibBasedArguments) {
    compilerArgument {
        name = "Xklib-relative-path-base"
        compilerName = "relativePathBases"
        description = """Relativize all the paths stored in a klib using the given path prefixes.
The supplied prefixes should be absolute paths to the directories containing the source code files.
Note: The prefixes are applied in the same order as they are passed in this CLI argument.""".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xklib-normalize-absolute-path"
        compilerName = "normalizeAbsolutePath"
        description = "Normalize absolute paths in klibs.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xklib-enable-signature-clash-checks"
        compilerName = "enableSignatureClashChecks"
        description = "Enable signature uniqueness checks.".asReleaseDependent()
        argumentType = BooleanType.defaultTrue

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xpartial-linkage"
        compilerName = "partialLinkageMode"
        description = "Use partial linkage mode.".asReleaseDependent()
        argumentType = StringType.defaultNull
        argumentTypeDescription = "{enable|disable}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xpartial-linkage-loglevel"
        compilerName = "partialLinkageLogLevel"
        description = "Define the compile-time log level for partial linkage.".asReleaseDependent()
        argumentType = StringType.defaultNull
        argumentTypeDescription = "{info|warning|error}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xklib-duplicated-unique-name-strategy"
        compilerName = "duplicatedUniqueNameStrategy"
        description = "Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.".asReleaseDependent()
        argumentType = StringType.defaultNull
        argumentTypeDescription = "{deny|allow-all-with-warning|allow-first-with-warning}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
        )
    }

    compilerArgument {
        name = "Xklib-ir-inliner"
        compilerName = "irInlinerBeforeKlibSerialization"
        description = """Set the mode of the experimental IR inliner on the first compilation stage.
- `intra-module` mode enforces inlining of the functions only from the compiled module
- `full` mode enforces inlining of all functions (from the compiled module and from all dependencies)
   Warning: This mode will trigger setting the `pre-release` flag for the compiled library.
- `disabled` mode completely disables the IR inliner
- `default` mode lets the IR inliner run in `intra-module`, `full` or `disabled` mode based on the current language version
        """.asReleaseDependent()
        argumentType = KlibIrInlinerModeType()
        argumentTypeDescription = ReleaseDependent(
            current = KlibIrInlinerMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}") { it.modeState }
        )

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
        argumentType = StringType.defaultNull
        argumentTypeDescription = "<version>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xklib-zip-file-accessor-cache-limit"
        description = "Maximum number of klibs that can be cached during compilation. Default is 64.".asReleaseDependent()
        argumentType = IntType(
            defaultValue = 64.asReleaseDependent()
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0
        )
    }

    compilerArgument {
        name = "Xskip-library-special-compatibility-checks"
        compilerName = "skipLibrarySpecialCompatibilityChecks"
        description = "Skip library compatibility checks for stdlib and kotlin.test library.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }
}
