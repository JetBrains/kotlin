// !DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R> build(@BuilderInference block: TestInterface<R>.() -> Unit) {}

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

fun box(): String {
    build myLabel@ {
        emit("")
        val x = this@myLabel
        ""
    }

    return "OK"
}