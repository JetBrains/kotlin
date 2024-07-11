/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

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

// Nullable subject shouldn't be lowered.
fun nullable() {
    val x: EnumA? = null
    when(x) {
        EnumA.A -> sb.appendLine("fail")
        else    -> sb.appendLine("ok")
    }
}

// Operator overloading won't trick us!
fun operatorOverloading() {
    operator fun E.contains(other: E): Boolean = false

    val y = E.ONE
    when(y) {
        in E.ONE    -> sb.appendLine("Should not reach here")
        else        -> sb.appendLine("ok")
    }
}

fun smoke1() {
    when (produceEntry()) {
        EnumA.B -> sb.appendLine("error")
        EnumA.A -> sb.appendLine("ok")
        EnumA.C -> sb.appendLine("error")
    }
}

fun smoke2() {
    when (produceEntry()) {
        EnumA.B -> sb.appendLine("error")
        else    -> sb.appendLine("ok")
    }
}

fun eA() = EnumA.A

fun eB() = EnumA.B


fun nestedWhen() {
    sb.appendLine(when (eA()) {
        EnumA.A, EnumA.C -> when (eB()) {
            EnumA.B -> "ok"
            else -> "nope"
        }
        else -> "nope"
    })
}

fun box(): String {
    nullable()
    operatorOverloading()
    smoke1()
    smoke2()
    nestedWhen()

    assertEquals("""
        ok
        ok
        ok
        ok
        ok

    """.trimIndent(), sb.toString())
    return "OK"
}