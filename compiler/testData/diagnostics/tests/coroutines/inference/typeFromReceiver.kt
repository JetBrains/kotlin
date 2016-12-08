// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class GenericController<T>

fun <S> generate(g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

suspend fun GenericController<List<String>>.test() {}

val test1 = generate {
    test()
}