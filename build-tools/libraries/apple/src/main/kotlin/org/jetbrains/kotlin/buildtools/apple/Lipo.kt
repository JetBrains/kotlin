/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/** `lipo`, which merges binaries for different architectures into one universal binary. */
public class Lipo internal constructor(private val tools: AppleTools) {

    /** `lipo -create <inputs> -output <output>`. */
    public fun createInvocation(inputs: List<Path>, output: Path): AppleToolInvocation {
        require(inputs.isNotEmpty()) { "lipo -create needs at least one input" }
        return tools.toolchainInvocation(
            "lipo",
            listOf("-create") + inputs.map { it.toString() } + listOf("-output", output.toString()),
        )
    }

    /** Runs [createInvocation]. */
    public fun create(inputs: List<Path>, output: Path) {
        tools.runner.runChecked(createInvocation(inputs, output))
    }
}
