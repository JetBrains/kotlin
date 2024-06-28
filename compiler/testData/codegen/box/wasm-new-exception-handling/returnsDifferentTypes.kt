// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

class ReceiveChannel<out E>

inline fun <E, R> ReceiveChannel<E>.consume(block: ReceiveChannel<E>.() -> R): R {
    try {
        return block()
    }
    finally {
        sb.appendLine("zzz")
    }
}

inline fun <E> ReceiveChannel<E>.elementAtOrElse(index: Int, defaultValue: (Int) -> E): E =
        consume {
            if (index < 0)
                return defaultValue(index)
            return 42 as E
        }

fun <E> ReceiveChannel<E>.elementAt(index: Int): E =
        elementAtOrElse(index) { throw IndexOutOfBoundsException("qxx") }

fun box(): String {
    sb.appendLine(ReceiveChannel<Int>().elementAt(0))

    assertEquals("""
        zzz
        42

    """.trimIndent(), sb.toString())
    return "OK"
}