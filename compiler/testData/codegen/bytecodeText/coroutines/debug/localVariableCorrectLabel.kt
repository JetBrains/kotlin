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
// 1 LOCALVARIABLE a I L[0-9]+ L20
// 1 LINENUMBER 9 L20

/* TODO: JVM_IR does not generate LINENUMBER at the end of the lambda */
// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE a I L[0-9]+ L5
// 1 LINENUMBER 8 L14
