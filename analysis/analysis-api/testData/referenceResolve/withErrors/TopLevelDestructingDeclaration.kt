// IGNORE_FE10

fun <T : Any, Z> createTuple(a: T, b: Z&Any): Pair<T, Z&Any> {
    return Pair(a, b)
}

fun main()
var (<caret>val1, val2) = createTuple<String, Int?>("a", 1)
}