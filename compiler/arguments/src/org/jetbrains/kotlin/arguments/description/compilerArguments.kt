/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.Modifier
import org.jetbrains.kotlin.arguments.dsl.base.compilerArguments

val kotlinCompilerArguments = compilerArguments {
    topLevel(
        name = CompilerArgumentsLevelNames.commonToolArguments,
        arguments = commonToolsArguments
    ) {
        subLevel(
            name = CompilerArgumentsLevelNames.commonCompilerArguments,
            arguments = commonCompilerArguments
        ) {
            subLevel(
                name = CompilerArgumentsLevelNames.jvmCompilerArguments,
                arguments = jvmCompilerArguments
            ) {}
            subLevel(
                name = CompilerArgumentsLevelNames.commonKlibBasedArguments,
                arguments = commonKlibBasedArguments
            ) {
                subLevel(
                    name = CompilerArgumentsLevelNames.commonJsAndWasmArguments,
                    arguments = commonJsAndWasmArguments
                ) {
                    modifier(Modifier.SEALED)
                    subLevel(
                        name = CompilerArgumentsLevelNames.legacyWasmArguments,
                        arguments = wasmArguments
                    ) {
                        modifier(Modifier.DEPRECATED)
                        modifier(Modifier.SEALED)
                        subLevel(
                            name = CompilerArgumentsLevelNames.jsArguments,
                            arguments = jsArguments
                        ) {}
                    }
                    subLevel(
                        name = CompilerArgumentsLevelNames.wasmArguments,
                        arguments = wasmArguments
                    ) {}
                }
                subLevel(
                    name = CompilerArgumentsLevelNames.nativeArguments,
                    arguments = nativeArguments
                ) {}
            }
            subLevel(
                name = CompilerArgumentsLevelNames.metadataArguments,
                arguments = metadataArguments
            ) {}
        }
    }
}
