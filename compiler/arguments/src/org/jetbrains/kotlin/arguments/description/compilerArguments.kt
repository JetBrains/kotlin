/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.description.removed.*
import org.jetbrains.kotlin.arguments.dsl.base.Modifier
import org.jetbrains.kotlin.arguments.dsl.base.compilerArguments

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
                    name = CompilerArgumentsLevelNames.commonJsWasmArguments,
                    mergeWith = setOf(actualCommonJsWasmArguments)
                ) {
                    subLevel(
                        name = CompilerArgumentsLevelNames.legacyWasmArguments,
                        mergeWith = setOf(actualWasmArguments, removedWasmArguments)
                    ) {
                        modifier(Modifier.DEPRECATED)
                        subLevel(
                            name = CompilerArgumentsLevelNames.jsArguments,
                            mergeWith = setOf(actualJsArguments, removedJsArguments)
                        ) {}
                    }
                    subLevel(
                        name = CompilerArgumentsLevelNames.wasmArguments,
                        mergeWith = setOf(actualWasmArguments, removedWasmArguments)
                    ) {}
                }
                subLevel(
                    name = CompilerArgumentsLevelNames.nativeArguments,
                    mergeWith = setOf(actualNativeArguments, removedNativeArguments)
                ) {}
            }
            subLevel(
                name = CompilerArgumentsLevelNames.metadataArguments,
                mergeWith = setOf(actualMetadataArguments, removedMetadataArguments)
            ) {}
        }
    }
}
