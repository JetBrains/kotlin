/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val actualNativeKlibArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.nativeKlibArguments) {
    compilerArgument {
        name = "produce"
        shortName = "p"
        description = "Specify the output file kind.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{program|static|dynamic|framework|library|bitcode}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "target"
        description = "Set the hardware target.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<target>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }
}