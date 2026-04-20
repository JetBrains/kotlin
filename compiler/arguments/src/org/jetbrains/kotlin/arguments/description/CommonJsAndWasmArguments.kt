/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.*

val actualCommonJsAndWasmArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonJsAndWasmArguments) {
    compilerArgument {
        name = "Xir-produce-js"
        description = "Generate a JS file using the IR backend.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }

    compilerArgument {
        name = "source-map"
        description = "Generate a source map.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "Xinclude"
        compilerName = "includes"
        description = "Path to an intermediate library that should be processed in the same manner as source files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        argumentType = PathType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "ir-output-dir"
        compilerName = "outputDir"
        description = "Destination for generated files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<directory>".asReleaseDependent()
        argumentType = PathType.defaultNull

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
        name = "nopack"
        description = "Don't pack the library into a klib file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }

    compilerArgument {
        name = "Xir-produce-klib-file"
        description = ReleaseDependent(
            """
                Generate a packed klib into the directory specified by '-ir-output-dir'.
                
                This argument is deprecated. Producing a packed klib is now the default behavior. 
                
                The '-nopack' argument can be used instead to determine if a packed klib file will be produced.
                Setting this argument to something other than `null` overrides the value from '-nopack'.
            """.trimIndent(),
            KotlinReleaseVersion.v1_3_70..KotlinReleaseVersion.v2_4_20 to
                    "Generate a packed klib into the directory specified by '-ir-output-dir'."
        )
        valueType = BooleanType(
            isNullable = ReleaseDependent(
                true,
                KotlinReleaseVersion.v1_3_70..KotlinReleaseVersion.v2_4_20 to false,
            ),
            defaultValue = ReleaseDependent(
                null,
                KotlinReleaseVersion.v1_3_70..KotlinReleaseVersion.v2_4_20 to false,
            ),
        )
        additionalAnnotations(
            Deprecated("Producing a packed klib is now the default behavior. The '-nopack' argument can be used instead to determine if a packed klib file will be produced."),
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
            deprecatedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "libraries"
        description =
            "Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        argumentType = SearchPathType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
            stabilizedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "Xfriend-modules"
        description = "Paths to friend modules.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        argumentType = SearchPathType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "Xcache-directory"
        description = "Path to the cache directory.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        argumentType = PathType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "Xir-produce-klib-dir"
        description = ReleaseDependent(
            """
                Generate an unpacked klib into the directory specified by '-ir-output-dir'.
                
                This argument is deprecated.
                 
                The '-nopack' argument should be used to determine if a packed klib file will be produced.
                Setting this argument to something other than `null` overrides the value from '-nopack'.
            """.trimIndent(),
            KotlinReleaseVersion.v1_3_70..KotlinReleaseVersion.v2_4_20 to
                    "Generate an unpacked klib into the parent directory of the output JS file."
        )
        valueType = BooleanType(
            isNullable = ReleaseDependent(
                true,
                KotlinReleaseVersion.v1_3_70..KotlinReleaseVersion.v2_4_20 to false,
            ),
            defaultValue = ReleaseDependent(
                null,
                KotlinReleaseVersion.v1_3_70..KotlinReleaseVersion.v2_4_20 to false,
            ),
        )
        additionalAnnotations(
            Deprecated("Use '-nopack' instead to determine if a packed klib file will be produced."),
        )
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
            deprecatedVersion = KotlinReleaseVersion.v2_4_20,
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

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "main"
        description = "Specify whether the 'main' function should be called upon execution.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{call|noCall}".asReleaseDependent()
        argumentType = JsMainCallModeType()

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

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "source-map-base-dirs"
        deprecatedName = "source-map-source-roots"
        description = "Base directories for calculating relative paths to source files in the source map.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        argumentType = SearchPathType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_60,
            stabilizedVersion = KotlinReleaseVersion.v1_1_60,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        /**
         * SourceMapEmbedSources should be null by default, since it has effect only when source maps are enabled.
         * When sourceMapEmbedSources are not null and source maps is disabled warning is reported.
         */
        name = "source-map-embed-sources"
        description = "Embed source files into the source map.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{always|never|inlining}".asReleaseDependent()
        argumentType = SourceMapEmbedSourcesType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
            stabilizedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "source-map-names-policy"
        description = "Mode for mapping generated names to original names.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{no|simple-names|fully-qualified-names}".asReleaseDependent()
        argumentType = SourceMapNamesPolicyType()

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
        affectsCompilationOutcome = false

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

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "Xir-dce-runtime-diagnostic"
        description = "Enable runtime diagnostics instead of removing declarations when performing DCE.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{log|exception}".asReleaseDependent()
        argumentType = JsIrDiagnosticModeType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
        )
    }

    compilerArgument {
        name = "Xgenerate-dts"
        description = "Generate a TypeScript declaration .d.ts file alongside the JS file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }

    compilerArgument {
        name = "Xstrict-implicit-export-types"
        compilerName = "strictImplicitExportType"
        description = "Generate strict types for implicitly exported entities inside d.ts files.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
        )
    }
}
