// LANGUAGE_VERSION: 1.3

import kotlin.coroutines.*
import kotlin.sequences.*

fun main(args: Array<String>) {
    val s = buildSequence {
        yield(1)
        val a = awaitSeq()
        println(a) // (1)
    }
    println(s.toList())
}

suspend fun SequenceBuilder<Int>.awaitSeq(): Int = 42

// 1 LOCALVARIABLE a I L18 L22 3
// 1 LINENUMBER 10 L18