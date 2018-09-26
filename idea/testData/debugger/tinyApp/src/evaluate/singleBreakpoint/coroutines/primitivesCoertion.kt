package primitivesCoertion

import kotlin.sequences.*
import kotlin.experimental.ExperimentalTypeInference

fun main(args: Array<String>) {
    val a = sequence {
        yield(1)
        val a = awaitSeq()
        //Breakpoint!
        println(a) // (1)
    }
    println(a.toList())
}

@UseExperimental(ExperimentalTypeInference::class)
@BuilderInference
suspend fun SequenceScope<Int>.awaitSeq(): Int = 42

// EXPRESSION: a
// RESULT: 42: I
