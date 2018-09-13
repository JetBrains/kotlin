// LANGUAGE_VERSION: 1.3

import kotlin.sequences.*

fun main(args: Array<String>) {
    val s = sequence {
        yield(1)
        val a = awaitSeq()
        println(a) // (1)
    }
    println(s.toList())
}

suspend fun SequenceScope<Int>.awaitSeq(): Int = 42

// 1 LOCALVARIABLE a I L18 L22 3
// 1 LINENUMBER 9 L18