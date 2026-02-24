/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description.removed

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val removedJsArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.jsArguments) {
    compilerArgument {
        name = "Xtyped-arrays"
        description = """This option does nothing and is left for compatibility with the legacy backend.
It is deprecated and will be removed in a future release.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
            deprecatedVersion = KotlinReleaseVersion.v2_1_0,
            removedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "output"
        compilerName = "outputFile"
        valueType = StringType.defaultNull
        description = "".asReleaseDependent()
        valueDescription = "<filepath>".asReleaseDependent()

        additionalAnnotations(
            Deprecated("It is senseless to use with IR compiler. Only for compatibility."),
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
            deprecatedVersion = KotlinReleaseVersion.v2_1_0,
            removedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }
}
