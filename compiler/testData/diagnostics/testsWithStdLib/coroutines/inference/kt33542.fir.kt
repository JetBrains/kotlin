// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !LANGUAGE: +NewInference

import kotlin.experimental.ExperimentalTypeInference

interface In<in E> {
    fun send(element: E)
}

class InImpl<E> : In<E> {
    override fun send(element: E) {}
}

@OptIn(ExperimentalTypeInference::class)
public fun <T> builder(@BuilderInference block: In<T>.() -> Unit) {
    InImpl<T>().block()
}

suspend fun yield() {}

fun test() {
    builder {
        <!INAPPLICABLE_CANDIDATE!>send<!>(run {
            yield() // No error but `yield` is not inside "suspension" context actually
        })
    }
}
