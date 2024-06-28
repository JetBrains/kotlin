/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import java.util.stream.Stream

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    // Determine main class name while still on main thread.
    val mainClassName = TestGeneratorUtil.getMainClassName()

    Stream.of(::generateJUnit3CompilerTests, ::generateJUnit5CompilerTests)
        .parallel()
        .forEach { it.invoke(args, mainClassName) }
}
