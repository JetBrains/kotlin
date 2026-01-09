/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringType
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.config.LanguageFeature

val actualCommonJsArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonJsArguments) {
    compilerArgument {
        name = "source-map"
        description = "Generate a source map.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "Xinclude"
        compilerName = "includes"
        description = "Path to an intermediate library that should be processed in the same manner as source files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    compilerArgument {
        name = "ir-output-dir"
        compilerName = "outputDir"
        description = "Destination for generated files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<directory>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
            stabilizedVersion = KotlinReleaseVersion.v1_8_20
        )
    }

    compilerArgument {
        name = "ir-output-name"
        compilerName = "moduleName"
        description = "Base name of generated files.".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
            stabilizedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "Xir-module-name"
        description = "Specify the name of the compilation module for the IR backend.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    compilerArgument {
        name = "Xir-produce-klib-file"
        description = "Generate a packed klib into the directory specified by '-ir-output-dir'.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }

    compilerArgument {
        name = "libraries"
        description = "Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
            stabilizedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    compilerArgument {
        name = "Xfriend-modules"
        description = "Paths to friend modules.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
        )
    }

    compilerArgument {
        name = "Xcache-directory"
        description = "Path to the cache directory.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "Xir-produce-klib-dir"
        description = "Generate an unpacked klib into the parent directory of the output JS file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }

    compilerArgument {
        name = "Xir-property-lazy-initialization"
        description = "Perform lazy initialization for properties.".asReleaseDependent()
        valueType = BooleanType(
            isNullable = false.asReleaseDependent(),
            defaultValue = true.asReleaseDependent()
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xir-dce"
        description = "Perform experimental dead code elimination.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }

    compilerArgument {
        name = "Xir-per-module-output-name"
        description = "Add a custom output name to the split .js files.".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
        )
    }

    compilerArgument {
        name = "main"
        description = "Specify whether the 'main' function should be called upon execution.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{call|noCall}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }
    compilerArgument {
        name = "source-map-prefix"
        description = "Add the specified prefix to the paths in the source map.".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
            stabilizedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    compilerArgument {
        name = "source-map-base-dirs"
        deprecatedName = "source-map-source-roots"
        description = "Base directories for calculating relative paths to source files in the source map.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_60,
            stabilizedVersion = KotlinReleaseVersion.v1_1_60,
        )
    }

    compilerArgument {
        /**
         * SourceMapEmbedSources should be null by default, since it has effect only when source maps are enabled.
         * When sourceMapEmbedSources are not null and source maps is disabled warning is reported.
         */
        name = "source-map-embed-sources"
        description = "Embed source files into the source map.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{always|never|inlining}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
            stabilizedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    compilerArgument {
        name = "source-map-names-policy"
        description = "Mode for mapping generated names to original names.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{no|simple-names|fully-qualified-names}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
            stabilizedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }
    compilerArgument {
        name = "Xfriend-modules-disabled"
        description = "Disable internal declaration export.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
        )
    }

    compilerArgument {
        name = "Xir-dce-print-reachability-info"
        description = "Print reachability information about declarations to 'stdout' while performing DCE.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    compilerArgument {
        name = "Xfake-override-validator"
        description = "Enable the IR fake override validator.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xir-dce-runtime-diagnostic"
        description = "Enable runtime diagnostics instead of removing declarations when performing DCE.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{log|exception}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
        )
    }


}
