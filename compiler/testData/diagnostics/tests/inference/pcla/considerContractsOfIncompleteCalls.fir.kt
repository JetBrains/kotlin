// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Controller<T> {
    fun yield(t: T): Boolean = true
}

@OptIn(ExperimentalContracts::class)
fun <R, B : Any> Controller<R>.ensureNotNull(value: B?, shift: () -> R): B {
    contract { returns() implies (value != null) }
    return value!!
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun bar(x: Int) {}

fun foo(x: Int?) {
    generate {
        ensureNotNull(x) { "" }
        bar(x)
    }
}
