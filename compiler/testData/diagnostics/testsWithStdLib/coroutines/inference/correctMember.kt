// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

interface Controller<T> {
    suspend fun yield(t: T) {}

    fun justString(): String = ""

    fun <Z> generidFun(t: Z) = t
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test1 = generate {
    yield(justString())
}

val test2 = generate {
    yield(generidFun(2))
}

