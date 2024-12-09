/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.asReleaseDependent
import org.jetbrains.kotlin.arguments.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.types.KotlinJvmTargetType

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val actualJvmCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.jvmCompilerArguments) {
    compilerArgument {
        name = "jvm-target"
        description = "Target version of the generated JVM bytecode (1.6 or 1.8).".asReleaseDependent()

        valueType = KotlinJvmTargetType()
        valueDescription = "<version>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0
        )
    }
}
