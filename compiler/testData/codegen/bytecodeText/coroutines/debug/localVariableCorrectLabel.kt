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
@BuilderInference
suspend fun SequenceScope<Int>.awaitSeq(): Int = 42

// 1 LOCALVARIABLE a I L17 L.* 3
// 1 LINENUMBER 8 L17
// Adding ignore flags below the test since the test relies on line numbers.
// IGNORE_BACKEND: JVM_IR

