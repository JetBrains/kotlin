// FIR_IDENTICAL
class Controller<T> {
    fun yield(t: T): String = "1"
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

class MyAsserter<E : MyAsserter<E>> {
    fun self(): E = TODO()
    fun member() {}
}

fun assertThat(x: String): MyAsserter<*> = TODO()

fun bar(): String? = null

fun check(b: Boolean, l: List<String>) {
    generate {
        assertThat("")
            .self()
            .member()

        assertThat(bar()!!)
            .self()
            .member()

        assertThat(yield(""))
            .self()
            .member()
    }.length
}
