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

// 1 LINENUMBER 9 L18
// 1 LOCALVARIABLE a I L[0-9]+ L17

// IGNORE_BACKEND_FIR: JVM_IR
