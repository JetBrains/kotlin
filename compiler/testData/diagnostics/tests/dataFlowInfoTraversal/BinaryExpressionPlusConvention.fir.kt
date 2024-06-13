// CHECK_TYPE

interface A

interface B : A
operator fun B.plus(b: B) = if (this == b) b else this

fun foo(a: A): B {
    val result = (a as B) + a
    checkSubtype<B>(a)
    return result
}

fun bar(a: A, b: B): B {
    val result = b + (a as B)
    checkSubtype<B>(a)
    return result
}
