// LANGUAGE_VERSION: 1.3

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

@UseExperimental(ExperimentalTypeInference::class)
@BuilderInference
suspend fun SequenceScope<Int>.awaitSeq(): Int = 42

// 1 LOCALVARIABLE a I L19 L23 3
// 1 LINENUMBER 10 L19
