/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.StringType

/**
 * Arguments specific to the Native klib compilation pipeline.
 * These arguments are only available when using the phased CLI for klib compilation.
 */
val actualNativeKlibArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.nativeKlibArguments) {
    compilerArgument {
        name = "Xnative-stdlib-path"
        description = "Path to the Native stdlib klib.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xnative-platform-libraries-path"
        description = "Path to the Native platform libraries directory.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }
}
