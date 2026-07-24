/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description.removed

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.ReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.previous
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.SearchPathType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val removedJvmCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.jvmCompilerArguments) {
    compilerArgument {
        name = "Xir-inliner"
        compilerName = "enableIrInliner"
        description = "Inline functions using the IR inliner instead of the bytecode inliner.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            removedVersion = KotlinReleaseVersion.v2_3_0
        )
    }

    compilerArgument {
        name = "Xuse-k2-kapt"
        description = "Enable the experimental support for K2 KAPT.".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            removedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xcompile-builtins-as-part-of-stdlib"
        compilerName = "expectBuiltinsAsPartOfStdlib"
        description = "Enable behaviour needed to compile builtins as part of JVM stdlib".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
            removedVersion = KotlinReleaseVersion.v2_3_20,
        )
    }

    compilerArgument {
        name = "Xuse-javac"
        description = "Use javac for Java source and class file analysis.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xcompile-java"
        description = "Reuse 'javac' analysis and compile Java source files.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_50,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xjavac-arguments"
        description = "Java compiler arguments.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<option[,]>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xserialize-ir"
        description = "Save the IR to metadata (Experimental).".asReleaseDependent()
        valueType = StringType(
            isNullable = false.asReleaseDependent(),
            defaultValue = "none".asReleaseDependent()
        )
        valueDescription = "{none|inline|all}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
            removedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xvalue-classes"
        description = "Enable experimental value classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
            removedVersion = KotlinReleaseVersion.v2_4_20,
        )
    }

    @OptIn(ExperimentalArgumentApi::class)
    compilerArgument {
        name = "Xklib"
        compilerName = "klibLibraries"
        description = "Paths to cross-platform libraries in the .klib format.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        argumentType = SearchPathType.defaultNull
        delimiter = KotlinCompilerArgument.Delimiter.PathSeparator

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        name = "Xlink-via-signatures"
        description = """Link JVM IR symbols via signatures instead of descriptors.
This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.
This option is deprecated and will be deleted in future versions.
It has no effect when -language-version is 2.0 or higher.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            deprecatedVersion = KotlinReleaseVersion.v2_0_0,
            removedVersion = KotlinReleaseVersion.v2_5_0,
        )
    }

    compilerArgument {
        val commonDescriptionPart = "Suppress warnings about deprecated JVM target versions."
        name = "Xsuppress-deprecated-jvm-target-warning"
        val introducedVersion = KotlinReleaseVersion.v1_5_0
        val deprecatedVersion = KotlinReleaseVersion.v1_7_20 // According to https://github.com/JetBrains/kotlin/commit/2e515f39456be7a8a0f9e15b9456c883b032375e
        val removedVersion = KotlinReleaseVersion.v2_5_0
        description = ReleaseDependent(
            commonDescriptionPart,
            deprecatedVersion..removedVersion.previous!! to """$commonDescriptionPart
This option has no effect and will be deleted in a future version.""",
            introducedVersion..deprecatedVersion.previous!! to commonDescriptionPart,
        )
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = introducedVersion,
            deprecatedVersion = deprecatedVersion,
            removedVersion = removedVersion,
        )
    }
}
