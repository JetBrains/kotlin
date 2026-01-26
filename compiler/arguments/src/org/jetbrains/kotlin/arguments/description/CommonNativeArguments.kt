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

/**
 * Arguments shared between full native compilation (nativeArguments) and klib-only compilation (nativeKlibArguments).
 */
val actualCommonNativeArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonNativeArguments) {
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

    compilerArgument {
        name = "module-name"
        deprecatedName = "module_name"
        description = "Specify a name for the compilation module.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "library"
        compilerName = "libraries"
        shortName = "l"
        description = "Link with the given library.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "nostdlib"
        description = "Don't link with the stdlib.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "no-default-libs"
        compilerName = "nodefaultlibs"
        deprecatedName = "nodefaultlibs"
        description = "Don't link the libraries from dist/klib automatically.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "friend-modules"
        description = "Paths to friend modules.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "Xrefines-paths"
        description = "Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize).".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "include-binary"
        compilerName = "includeBinaries"
        deprecatedName = "includeBinary"
        shortName = "ib"
        description = "Pack the given external binary into the klib.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "manifest"
        compilerName = "manifestFile"
        description = "Provide a manifest addend file.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "nopack"
        description = "Don't pack the library into a klib file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "output"
        compilerName = "outputName"
        shortName = "o"
        description = "Output name.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "Xinclude"
        compilerName = "includes"
        description = "A path to an intermediate library that should be processed in the same manner as source files.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "Xshort-module-name"
        description = "A short name used to denote this library in the IDE and in a generated Objective-C header.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "Xwrite-dependencies-of-produced-klib-to"
        description = "Write file containing the paths of dependencies used during klib compilation to the provided path".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xmanifest-native-targets"
        description =
            "Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xexport-kdoc"
        compilerName = "exportKDoc"
        description = "Export KDoc entries in the framework header.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    }

    compilerArgument {
        name = "Xheader-klib-path"
        description = "Save a klib that only contains the public ABI to the given path.".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_24,
        )
    }
}
