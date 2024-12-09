/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.asReleaseDependent
import org.jetbrains.kotlin.arguments.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.types.KotlinVersionType

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val actualCommonCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonCompilerArguments) {
    subLevel("commonCompilerArguments", mergeWith = setOf(deprecatedCommonArgs)) {
        compilerArgument {
            name = "language-version"
            description = "Provide source compatibility with the specified version of Kotlin.".asReleaseDependent()

            valueType = KotlinVersionType()
            valueDescription = "<version>".asReleaseDependent()

            lifecycle(
                introducedVersion = KotlinReleaseVersion.v1_9_20
            )
        }

        compilerArgument {
            name = "api-version"
            description = "Allow using declarations from only the specified version of bundled libraries.".asReleaseDependent()

            valueType = KotlinVersionType()
            valueDescription = "<version>".asReleaseDependent()

            lifecycle(
                introducedVersion = KotlinReleaseVersion.v1_4_0
            )
        }
    }
}
