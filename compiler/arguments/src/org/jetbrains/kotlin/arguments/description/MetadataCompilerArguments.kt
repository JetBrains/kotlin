/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val actualMetadataArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.metadataArguments) {
    compilerArgument {
        name = "d"
        compilerName = "destination"
        description = "Destination for generated .kotlin_metadata files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<directory|jar>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
            stabilizedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    compilerArgument {
        name = "classpath"
        shortName = "cp"
        description = "List of directories and JAR/ZIP archives to search for user .kotlin_metadata files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
            stabilizedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    compilerArgument {
        name = "module-name"
        description = "Name of the generated .kotlin_module file.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_20,
            stabilizedVersion = KotlinReleaseVersion.v1_2_20,
        )
    }

    compilerArgument {
        name = "Xfriend-paths"
        description = "Paths to output directories for friend modules (modules whose internals should be visible).".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_71
        )
    }

    compilerArgument {
        name = "Xrefines-paths"
        description = "Paths to output directories for refined modules (modules whose expects this module can actualize).".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    compilerArgument {
        name = "Xlegacy-metadata-jar-k2"
        compilerName = "legacyMetadataJar"
        description = "Produce a legacy metadata jar instead of metadata klib. Suitable only for K2 compilation".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
        )
    }

    compilerArgument {
        name = "Xtarget-platform"
        description = "Target platform for metadata generation. Possible values: JVM, JS, WasmJs, WasmWasi, Native".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_20,
        )
    }
}
