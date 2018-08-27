/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.for_loops_let_with_nullable

import kotlin.test.*

// Github issue #1012
fun testInt(left: Int?, right: Int?, step: Int?) {
    right?.let {
        for (i in 0..it) { print(i) }
    }
    println()

    left?.let {
        for (i in it..5) { print(i) }
    }
    println()

    step?.let {
        for (i in 0..5 step it) { print(i) }
    }
    println()

    right?.let {
        for (i in 0 until it) { print(i) }
    }
    println()

    left?.let {
        for (i in it until 5) { print(i) }
    }
    println()

    step?.let {
        for (i in 0 until 5 step it) { print(i) }
    }
    println()

    right?.let {
        for (i in it downTo 0) { print(i) }
    }
    println()

    left?.let {
        for (i in 5 downTo it) { print(i) }
    }
    println()

    step?.let {
        for (i in 5 downTo 0 step it) { print(i) }
    }
    println()
}

fun testLong(left: Long?, right: Long?, step: Long?) {
    right?.let {
        for (i in 0..it) { print(i) }
    }
    println()

    left?.let {
        for (i in it..5) { print(i) }
    }
    println()

    step?.let {
        for (i in 0..5L step it) { print(i) }
    }
    println()

    right?.let {
        for (i in 0 until it) { print(i) }
    }
    println()

    left?.let {
        for (i in it until 5) { print(i) }
    }
    println()

    step?.let {
        for (i in 0 until 5L step it) { print(i) }
    }
    println()

    right?.let {
        for (i in it downTo 0) { print(i) }
    }
    println()

    left?.let {
        for (i in 5 downTo it) { print(i) }
    }
    println()

    step?.let {
        for (i in 5 downTo 0L step it) { print(i) }
    }
    println()
}

fun testChar(left: Char?, right: Char?, step: Int?) {
    right?.let {
        for (i in 'a'..it) { print(i) }
    }
    println()

    left?.let {
        for (i in it..'f') { print(i) }
    }
    println()

    step?.let {
        for (i in 'a'..'f' step it) { print(i) }
    }
    println()

    right?.let {
        for (i in 'a' until it) { print(i) }
    }
    println()

    left?.let {
        for (i in it until 'f') { print(i) }
    }
    println()

    step?.let {
        for (i in 'a' until 'f' step it) { print(i) }
    }
    println()

    right?.let {
        for (i in it downTo 'a') { print(i) }
    }
    println()

    left?.let {
        for (i in 'f' downTo it) { print(i) }
    }
    println()

    step?.let {
        for (i in 'f' downTo 'a' step it) { print(i) }
    }
    println()
}

@Test fun runTest() {
    testInt(0, 5, 2)
    testLong(0, 5, 2)
    testChar('a', 'f', 2)
}