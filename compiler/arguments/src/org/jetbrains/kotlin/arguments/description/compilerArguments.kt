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
        mergeWith = [actualCommonToolsArguments, removedCommonToolsArguments]
    ) {
        subLevel(
            name = CompilerArgumentsLevelNames.commonCompilerArguments,
            mergeWith = [actualCommonCompilerArguments, removedCommonCompilerArguments]
        ) {
            subLevel(
                name = CompilerArgumentsLevelNames.jvmCompilerArguments,
                mergeWith = [actualJvmCompilerArguments, removedJvmCompilerArguments]
            ) {}
            subLevel(
                name = CompilerArgumentsLevelNames.commonKlibBasedArguments,
                mergeWith = [actualCommonKlibBasedArguments, removedCommonKlibBasedCompilerArguments]
            ) {
                subLevel(
                    name = CompilerArgumentsLevelNames.commonJsAndWasmArguments,
                    mergeWith = [actualCommonJsAndWasmArguments]
                ) {
                    modifier(Modifier.SEALED)
                    subLevel(
                        name = CompilerArgumentsLevelNames.legacyWasmArguments,
                        mergeWith = [actualWasmArguments, removedWasmArguments]
                    ) {
                        modifier(Modifier.DEPRECATED)
                        modifier(Modifier.SEALED)
                        subLevel(
                            name = CompilerArgumentsLevelNames.jsArguments,
                            mergeWith = [actualJsArguments, removedJsArguments]
                        ) {}
                    }
                    subLevel(
                        name = CompilerArgumentsLevelNames.wasmArguments,
                        mergeWith = [actualWasmArguments, removedWasmArguments]
                    ) {}
                }
                subLevel(
                    name = CompilerArgumentsLevelNames.nativeArguments,
                    mergeWith = [actualNativeArguments, removedNativeArguments]
                ) {}
            }
            subLevel(
                name = CompilerArgumentsLevelNames.metadataArguments,
                mergeWith = [actualMetadataArguments, removedMetadataArguments]
            ) {}
        }
    }
}
