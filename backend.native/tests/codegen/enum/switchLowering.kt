/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.switchLowering

import kotlin.test.*

enum class EnumA {
    A, B, C
}

enum class EnumB {
    A, B
}

enum class E {
    ONE, TWO, THREE
}

fun produceEntry() = EnumA.A

// Check that we fail on comparison of different enum types.
fun differentEnums() {
    println(when (produceEntry()) {
        EnumB.A -> "EnumB.A"
        EnumA.A -> "EnumA.A"
        EnumA.B -> "EnumA.B"
        else    -> "nah"
    })
}

// Nullable subject shouldn't be lowered.
fun nullable() {
    val x: EnumA? = null
    when(x) {
        EnumA.A -> println("fail")
        else    -> println("ok")
    }
}

// Operator overloading won't trick us!
fun operatorOverloading() {
    operator fun E.contains(other: E): Boolean = false

    val y = E.ONE
    when(y) {
        in E.ONE    -> println("Should not reach here")
        else        -> println("ok")
    }
}

fun smoke1() {
    when (produceEntry()) {
        EnumA.B -> println("error")
        EnumA.A -> println("ok")
        EnumA.C -> println("error")
    }
}

fun smoke2() {
    when (produceEntry()) {
        EnumA.B -> println("error")
        else    -> println("ok")
    }
}

fun eA() = EnumA.A

fun eB() = EnumA.B


fun nestedWhen() {
    println(when (eA()) {
        EnumA.A, EnumA.C -> when (eB()) {
            EnumA.B -> "ok"
            else -> "nope"
        }
        else -> "nope"
    })
}

@Test fun runTest() {
    differentEnums()
    nullable()
    operatorOverloading()
    smoke1()
    smoke2()
    nestedWhen()
}