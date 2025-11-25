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

val actualJsArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.jsArguments) {
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
        name = "source-map"
        description = "Generate a source map.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

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
        name = "target"
        description = "Generate JS files for the specified ECMA version.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{ es5, es2015 }".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "Xir-keep"
        description = "Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), and for which to keep non-minified names.".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "module-kind"
        description = "The kind of JS module generated by the compiler. ES modules are enabled by default in case of ES2015 target usage".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{plain|amd|commonjs|umd|es}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_4,
            stabilizedVersion = KotlinReleaseVersion.v1_0_4,
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

    // Advanced options
    compilerArgument {
        name = "Xir-produce-klib-dir"
        description = "Generate an unpacked klib into the parent directory of the output JS file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
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
        name = "Xir-produce-js"
        description = "Generate a JS file using the IR backend.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
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
        name = "Xir-dce-runtime-diagnostic"
        description = "Enable runtime diagnostics instead of removing declarations when performing DCE.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{log|exception}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
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
        name = "Xir-minimized-member-names"
        description = "Minimize the names of members.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
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
        name = "Xir-safe-external-boolean"
        description = "Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30
        )
    }

    compilerArgument {
        name = "Xir-safe-external-boolean-diagnostic"
        description = "Enable runtime diagnostics when accessing external 'Boolean' properties.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{log|exception}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
        )
    }

    compilerArgument {
        name = "Xir-per-module"
        description = "Generate one .js file per module.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
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
        name = "Xir-per-file"
        description = "Generate one .js file per source file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_20,
        )
    }

    compilerArgument {
        name = "Xir-generate-inline-anonymous-functions"
        description = "Lambda expressions that capture values are translated into in-line anonymous JavaScript functions.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_20,
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
        name = "Xcache-directory"
        description = "Path to the cache directory.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "Xir-build-cache"
        description = "Use the compiler to build the cache.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
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
        name = "Xgenerate-polyfills"
        description = "Generate polyfills for features from the ES6+ standards.".asReleaseDependent()
        valueType = BooleanType(
            isNullable = false.asReleaseDependent(),
            defaultValue = true.asReleaseDependent()
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
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

    compilerArgument {
        name = "Xes-classes"
        compilerName = "useEsClasses"
        description = "Let generated JavaScript code use ES2015 classes. Enabled by default in case of ES2015 target usage".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "Xplatform-arguments-in-main-function"
        compilerName = "platformArgumentsProviderJsExpression"
        description = "JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }

    compilerArgument {
        name = "Xes-generators"
        compilerName = "useEsGenerators"
        description = "Enable ES2015 generator functions usage inside the compiled code. Enabled by default in case of ES2015 target usage".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }

    compilerArgument {
        name = "Xes-arrow-functions"
        compilerName = "useEsArrowFunctions"
        description = "Use ES2015 arrow functions in the JavaScript code generated for Kotlin lambdas. Enabled by default in case of ES2015 target usage".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
        )
    }

    compilerArgument {
        name = "Xes-long-as-bigint"
        compilerName = "compileLongAsBigInt"
        description = "Compile Long values as ES2020 bigint instead of object.".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20,
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
        name = "Xfriend-modules"
        description = "Paths to friend modules.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
        )
    }

    compilerArgument {
        name = "Xenable-extension-functions-in-externals"
        compilerName = "extensionFunctionsInExternals"
        description = "Enable extension function members in external interfaces.".asReleaseDependent()
        valueType = BooleanType.defaultFalse
        additionalAnnotations(Enables(LanguageFeature.JsEnableExtensionFunctionInExternals))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_32,
        )
    }

    compilerArgument {
        name = "Xenable-suspend-function-exporting"
        compilerName = "allowExportingSuspendFunctions"
        description = "Enable exporting suspend functions to JavaScript/TypeScript.".asReleaseDependent()
        valueType = BooleanType.defaultFalse
        additionalAnnotations(Enables(LanguageFeature.JsAllowExportingSuspendFunctions))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
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
        name = "Xoptimize-generated-js"
        description = "Perform additional optimizations on the generated JS code.".asReleaseDependent()
        valueType = BooleanType(
            isNullable = false.asReleaseDependent(),
            defaultValue = true.asReleaseDependent()
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
        )
    }
}
