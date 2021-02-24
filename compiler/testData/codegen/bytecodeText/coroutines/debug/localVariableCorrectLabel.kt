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


// 1 LOCALVARIABLE a I L[0-9]+ L18

/* TODO: JVM_IR does not generate LINENUMBER at the end of the lambda */
// JVM_TEMPLATES
// 1 LINENUMBER 9 L19


// IGNORE_BACKEND_FIR: JVM_IR
