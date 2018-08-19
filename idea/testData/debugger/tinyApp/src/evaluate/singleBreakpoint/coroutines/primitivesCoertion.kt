package primitivesCoertion

import kotlin.coroutines.*

fun main(args: Array<String>) {
    val a = buildSequence {
        yield(1)
        val a = awaitSeq()
        //Breakpoint!
        println(a) // (1)
    }
    println(a.toList())
}

suspend fun SequenceBuilder<Int>.awaitSeq(): Int = 42

// EXPRESSION: a
// RESULT: 42: I
