// CHECK_TYPE

infix fun Int.compareTo(o: Int) = 0

fun foo(a: Number): Int {
    val result = (a as Int) compareTo a
    checkSubtype<Int>(a)
    return result
}

fun bar(a: Number): Int {
    val result = 42 compareTo (a as Int)
    checkSubtype<Int>(a)
    return result
}