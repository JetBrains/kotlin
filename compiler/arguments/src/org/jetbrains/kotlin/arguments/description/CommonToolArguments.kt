/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.asReleaseDependent
import org.jetbrains.kotlin.arguments.compilerArgumentsLevel
import org.jetbrains.kotlin.arguments.defaultFalse
import org.jetbrains.kotlin.arguments.types.BooleanType

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val actualCommonToolsArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonToolArguments) {
    compilerArgument {
        name = "help"
        shortName = "h"
        description = "Print a synopsis of standard options.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0
        )
    }

    compilerArgument {
        name = "X"
        description = "Print a synopsis of advanced options.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20
        )
    }

    compilerArgument {
        name = "version"
        description = "Display the compiler version.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20
        )
    }

    compilerArgument {
        name = "verbose"
        description = "Enable verbose logging output.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20
        )
    }

    compilerArgument {
        name = "nowarn"
        description = "Don't generate any warnings.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20
        )
    }

    compilerArgument {
        name = "Werror"
        description = "Report an error if there are any warnings.".asReleaseDependent()

        valueType = BooleanType.defaultFalse
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20
        )
    }
}
