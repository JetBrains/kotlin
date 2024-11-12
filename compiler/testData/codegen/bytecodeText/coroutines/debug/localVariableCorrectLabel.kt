// API_VERSION: LATEST

import kotlin.sequences.*
import kotlin.experimental.*

fun main(args: Array<String>) {
    val s = sequence {
        yield(1)
        val a = awaitSeq()
        println(a) // (1)
    }
    println(s.toList())
}

@OptIn(ExperimentalTypeInference::class)
suspend fun SequenceScope<Int>.awaitSeq(): Int = 42

// 1 LINENUMBER 10 L12
// 1 LOCALVARIABLE a I L[0-9]+ L5
