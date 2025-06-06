/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.TemporaryCompilerArgumentLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.stubLifecycle
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes
import org.jetbrains.kotlin.cli.common.arguments.GradleOption

val actualCommonToolsArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonToolArguments) {
    compilerArgument {
        name = "help"
        shortName = "h"
        description = "Print a synopsis of standard options.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "X"
        compilerName = "extraHelp"
        description = "Print a synopsis of advanced options.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "version"
        description = "Display the compiler version.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "verbose"
        description = "Enable verbose logging output.".asReleaseDependent()

        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INTERNAL,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )
        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "nowarn"
        compilerName = "suppressWarnings"
        description = "Don't generate any warnings.".asReleaseDependent()

        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INTERNAL,
                shouldGenerateDeprecatedKotlinOptions = true,
            )

        )
        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Werror"
        compilerName = "allWarningsAsErrors"
        description = "Report an error if there are any warnings.".asReleaseDependent()

        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Wextra"
        compilerName = "extraWarnings"
        description = "Enable extra checkers for K2.".asReleaseDependent()

        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT,
            )
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }
}
