// !LANGUAGE: +CallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

inline fun myRun(block: () -> Unit): Unit {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

inline fun unknownRun(block: () -> Unit) { block() }

fun foo() {
    val x: Int
    myRun {
        unknownRun { println("shouldn't change anything") }
        x = 42
    }
    println(x)
}