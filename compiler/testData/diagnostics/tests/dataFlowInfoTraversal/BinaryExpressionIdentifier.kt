fun foo(a: Number): Int {
    val result = a as Int compareTo a
    a : Int
    return result
}

fun bar(a: Number): Int {
    val result = 42 compareTo a as Int
    a : Int
    return result
}
