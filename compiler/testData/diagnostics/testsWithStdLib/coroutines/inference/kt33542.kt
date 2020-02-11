// !LANGUAGE: +NewInference

import kotlin.experimental.ExperimentalTypeInference

interface In<in E> {
    fun send(element: E)
}

class InImpl<E> : In<E> {
    override fun send(element: E) {}
}

@<!DEPRECATION, EXPERIMENTAL_IS_NOT_ENABLED!>UseExperimental<!>(ExperimentalTypeInference::class)
public fun <T> builder(@BuilderInference block: In<T>.() -> Unit) {
    InImpl<T>().block()
}

suspend fun yield() {}

fun test() {
    builder {
        send(run {
            <!ILLEGAL_SUSPEND_FUNCTION_CALL!>yield<!>() // No error but `yield` is not inside "suspension" context actually
        })
    }
}
