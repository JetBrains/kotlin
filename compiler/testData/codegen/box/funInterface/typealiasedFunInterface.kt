fun interface Foo<A, B> {
    fun invoke(a: A): B
}

typealias TA<K> = Foo<Int, K>

class Bar<X, Y>(val y: Y) {
    fun foo(x: X): Y = y
}

typealias TB<M> = Bar<Int, M>

fun box() = "OK".also {
    TA { "string" }.invoke(20)
    TB("3").foo(3)

    val it: (Int) -> String = { "20" }
    it.let(::TA)

    "3".let(::TB)
}
