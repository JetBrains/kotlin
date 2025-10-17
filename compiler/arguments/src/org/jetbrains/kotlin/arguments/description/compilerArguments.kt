/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.description.removed.removedCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.removed.removedCommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.arguments.description.removed.removedCommonToolsArguments
import org.jetbrains.kotlin.arguments.description.removed.removedJsArguments
import org.jetbrains.kotlin.arguments.description.removed.removedJvmCompilerArguments
import org.jetbrains.kotlin.arguments.description.removed.removedMetadataArguments
import org.jetbrains.kotlin.arguments.description.removed.removedNativeArguments
import org.jetbrains.kotlin.arguments.description.removed.removedWasmArguments
import org.jetbrains.kotlin.arguments.dsl.base.compilerArguments
import removedNativeKlibArguments

val kotlinCompilerArguments = compilerArguments {
    topLevel(
        name = CompilerArgumentsLevelNames.commonToolArguments,
        mergeWith = setOf(actualCommonToolsArguments, removedCommonToolsArguments)
    ) {
        subLevel(
            name = CompilerArgumentsLevelNames.commonCompilerArguments,
            mergeWith = setOf(actualCommonCompilerArguments, removedCommonCompilerArguments)
        ) {
            subLevel(
                name = CompilerArgumentsLevelNames.jvmCompilerArguments,
                mergeWith = setOf(actualJvmCompilerArguments, removedJvmCompilerArguments)
            ) {}
            subLevel(
                name = CompilerArgumentsLevelNames.commonKlibBasedArguments,
                mergeWith = setOf(actualCommonKlibBasedArguments, removedCommonKlibBasedCompilerArguments)
            ) {
                subLevel(
                    name = CompilerArgumentsLevelNames.wasmArguments,
                    mergeWith = setOf(actualWasmArguments, removedWasmArguments)
                ) {
                    subLevel(
                        name = CompilerArgumentsLevelNames.jsArguments,
                        mergeWith = setOf(actualJsArguments, removedJsArguments)
                    ) {}
                }
                subLevel(
                    name = CompilerArgumentsLevelNames.nativeKlibArguments,
                    mergeWith = setOf(actualNativeKlibArguments, removedNativeKlibArguments)
                ) {
                    subLevel(
                        name = CompilerArgumentsLevelNames.nativeArguments,
                        mergeWith = setOf(actualNativeArguments, removedNativeArguments)
                    ) {}
                }
            }
            subLevel(
                name = CompilerArgumentsLevelNames.metadataArguments,
                mergeWith = setOf(actualMetadataArguments, removedMetadataArguments)
            ) {}
        }
    }
}
