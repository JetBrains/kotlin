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

// label numbers differ in BEs

// JVM_TEMPLATES
// 1 LOCALVARIABLE a I L17 L.*
// 1 LINENUMBER 8 L17

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE a I L14 L.*
// 1 LINENUMBER 8 L14
