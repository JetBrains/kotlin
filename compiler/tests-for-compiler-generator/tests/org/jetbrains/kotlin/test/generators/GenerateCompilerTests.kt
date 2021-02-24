/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateJUnit3CompilerTests(args)
    generateJUnit5CompilerTests(args)
}
