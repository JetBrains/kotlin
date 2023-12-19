// FIR_IDENTICAL

class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()
fun <T, R> T.bar(x: (T) -> R): R = TODO()

var children: String = TODO()

fun main(key: String?, v: Number) {
    generate {
        yield("")
        // Here, elvis-synthetic call is not completed yet, but it's variable K is already fixed
        // Assignment is used, because for that (outside of PCLA) we would use FULL completion
        children = key ?: v.bar { it.toString() }
    }.length
}

