/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/**
 * Apple's `libtool`, which creates static libraries.
 *
 * It runs through `xcrun`, so a GNU `libtool` earlier on `PATH` is never picked up.
 */
public class Libtool internal constructor(private val tools: AppleTools) {

    /** `libtool -static -o <output> <inputs>`. Inputs are object files or static libraries. */
    public fun createStaticInvocation(inputs: List<Path>, output: Path): AppleToolInvocation {
        require(inputs.isNotEmpty()) { "libtool -static needs at least one input" }
        return tools.toolchainInvocation(
            "libtool",
            listOf("-static", "-o", output.toString()) + inputs.map { it.toString() },
        )
    }

    /** Runs [createStaticInvocation]. */
    public fun createStatic(inputs: List<Path>, output: Path) {
        tools.runner.runChecked(createStaticInvocation(inputs, output))
    }
}
