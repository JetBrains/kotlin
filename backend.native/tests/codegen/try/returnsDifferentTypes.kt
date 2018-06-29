package codegen.`try`.returnsDifferentTypes

import kotlin.test.*

class ReceiveChannel<out E>

inline fun <E, R> ReceiveChannel<E>.consume(block: ReceiveChannel<E>.() -> R): R {
    try {
        return block()
    }
    finally {
        println("zzz")
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

@Test fun runTest() {
    println(ReceiveChannel<Int>().elementAt(0))
}