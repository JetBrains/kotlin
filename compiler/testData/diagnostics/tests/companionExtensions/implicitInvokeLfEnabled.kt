// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: test.kt
package my

import other.invoke
import other.invokable

class C
class C2 {
    companion object {
        operator fun invoke(x: Boolean) = x
        operator fun invoke(i: Int) = null
    }
}

class Invokable {
    operator fun invoke() {}
}

companion operator fun C.invoke(s: String) = s
companion operator fun C.invoke() = false
companion val C.invokable = Invokable()

fun testC() {
    val s: String = C("")
    val c: C = C()

    C.invokable()
}

fun testC2() {
    val i: Int = C2(1)
    val b: Boolean = C2(true)
    val c: C2 = C2()

    C2.invokable()
}

// FILE: other.kt
package other

import my.C2
import my.Invokable

companion operator fun C2.invoke(x: Int) = x
companion operator fun C2.invoke() = false
companion val C2.invokable = Invokable()

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, integerLiteral,
localProperty, objectDeclaration, operator, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
