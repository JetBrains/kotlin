package primitivesCoertion

import kotlin.sequences.*

fun main(args: Array<String>) {
    val a = sequence {
        yield(1)
        val a = awaitSeq()
        //Breakpoint!
        println(a) // (1)
    }
    println(a.toList())
}

suspend fun SequenceScope<Int>.awaitSeq(): Int = 42

// EXPRESSION: a
// RESULT: 42: I
