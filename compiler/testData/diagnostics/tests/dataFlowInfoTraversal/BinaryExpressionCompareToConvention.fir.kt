// CHECK_TYPE

interface A

interface B : A
operator fun B.compareTo(b: B) = if (this == b) 0 else 1

fun foo(a: A): Boolean {
    val result = (a as B) < a
    checkSubtype<B>(a)
    return result
}

fun bar(a: A, b: B): Boolean {
    val result = b < (a as B)
    checkSubtype<B>(a)
    return result
}
