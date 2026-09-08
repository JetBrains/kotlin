/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description.removed

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.*
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringListType
import org.jetbrains.kotlin.arguments.dsl.types.StringType
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.config.LanguageFeature

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

    compilerArgument {
        name = "Xinline-classes"
        description = "Enable experimental inline classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.InlineClasses)
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_50,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xnew-inference"
        description = "Enable the new experimental generic type inference algorithm.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.NewInference),
            Enables(LanguageFeature.SamConversionPerArgument),
            Enables(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType),
            Enables(LanguageFeature.DisableCompatibilityModeForNewInference),
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_20,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xunrestricted-builder-inference"
        description =
            "Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.UnrestrictedBuilderInference))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        val introducedVersion = KotlinReleaseVersion.v2_1_0
        val deprecatedVersion = KotlinReleaseVersion.v2_2_0 // According to https://github.com/JetBrains/kotlin/commit/533d2f5ba6e6d2759d92d59b6004ee433214e262
        val commonDescriptionPart = "Suppress specified warning module-wide."
        name = "Xsuppress-warning"
        compilerName = "suppressedDiagnostics"
        description = ReleaseDependent(
            commonDescriptionPart,
            deprecatedVersion..KotlinReleaseVersion.v2_4_20 to "$commonDescriptionPart This option is deprecated in favor of \"-Xwarning-level\" flag",
            introducedVersion..deprecatedVersion.previous!! to commonDescriptionPart,
        )
        valueDescription = "<WARNING_NAME>".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        argumentType = StringListType.defaultEmpty
        deprecatedMessage = "Use '-Xwarning-level=<WARNING_NAME>:disabled' instead (and the same for other warnings)."

        lifecycle(
            introducedVersion = introducedVersion,
            deprecatedVersion = deprecatedVersion,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xuse-fir-experimental-checkers"
        description = "Enable experimental frontend IR checkers that are not yet ready for production.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            deprecatedVersion = KotlinReleaseVersion.v2_2_20,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xintellij-plugin-root"
        description =
            "Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.".asReleaseDependent()
        valueDescription = "<path>".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
            deprecatedVersion = KotlinReleaseVersion.v2_4_20,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xno-check-actual"
        description = "Do not check for the presence of the 'actual' modifier in multiplatform projects.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_60,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xignore-const-optimization-errors"
        description = "Ignore all compilation exceptions while optimizing some constant expressions.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
        restrictedToCompilerPhase = KotlinCompilerPhase.BACKEND_COMPILATION
    }

    compilerArgument {
        name = "Xdirect-java-actualization"
        description = "Enable experimental direct Java actualization support.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.DirectJavaActualization))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }
}
