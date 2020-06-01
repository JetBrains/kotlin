// !CHECK_TYPE

infix fun Int.equals(o: Int) = false

fun foo(a: Number): Boolean {
    val result = (a as Int) equals a
    checkSubtype<Int>(a)
    return result
}

fun bar(a: Number): Boolean {
    val result = 42 equals (a as Int)
    checkSubtype<Int>(a)
    return result
}