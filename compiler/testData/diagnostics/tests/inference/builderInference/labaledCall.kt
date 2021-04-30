// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -UNCHECKED_CAST -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R> build(@BuilderInference block: TestInterface<R>.() -> Unit): R = TODO()

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

fun main() {
    build myLabel@ {
        emit("")
        val x = this@myLabel
        ""
    }
}