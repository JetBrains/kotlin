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
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val removedJvmCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.jvmCompilerArguments) {
    compilerArgument {
        name = "Xir-inliner"
        compilerName = "enableIrInliner"
        description = "Inline functions using the IR inliner instead of the bytecode inliner.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            removedVersion = KotlinReleaseVersion.v2_3_0
        )
    }

    compilerArgument {
        name = "Xuse-k2-kapt"
        description = "Enable the experimental support for K2 KAPT.".asReleaseDependent()
        argumentType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            removedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xcompile-builtins-as-part-of-stdlib"
        compilerName = "expectBuiltinsAsPartOfStdlib"
        description = "Enable behaviour needed to compile builtins as part of JVM stdlib".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
            removedVersion = KotlinReleaseVersion.v2_3_20,
        )
    }

    compilerArgument {
        name = "Xuse-javac"
        description = "Use javac for Java source and class file analysis.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xcompile-java"
        description = "Reuse 'javac' analysis and compile Java source files.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_50,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xjavac-arguments"
        description = "Java compiler arguments.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        argumentTypeDescription = "<option[,]>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xserialize-ir"
        description = "Save the IR to metadata (Experimental).".asReleaseDependent()
        argumentType = StringType(
            isNullable = false.asReleaseDependent(),
            defaultValue = "none".asReleaseDependent()
        )
        argumentTypeDescription = "{none|inline|all}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }
}
