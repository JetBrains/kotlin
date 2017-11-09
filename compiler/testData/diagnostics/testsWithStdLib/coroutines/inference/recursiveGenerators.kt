// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class GenericController<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

val test1 = generate {
    yield(generate {
        yield(generate {
            yield(generate {
                yield(3)
            })
        })
    })
}
