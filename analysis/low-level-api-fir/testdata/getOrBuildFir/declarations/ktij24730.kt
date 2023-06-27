fun <T : Any, Z> createTuple(a: T, b: Z&Any): Pair<T, Z&Any> {
    return Pair(a, b)
}

fun main()
    var (<expr>val1</expr>, val2) = createTuple<String, Int?>("a", 1)
}