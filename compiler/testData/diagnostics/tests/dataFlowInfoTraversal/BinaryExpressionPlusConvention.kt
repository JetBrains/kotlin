trait A

trait B : A
fun B.plus(b: B) = if (this == b) b else this

fun foo(a: A): B {
    val result = a as B + a
    a : B
    return result
}

fun bar(a: A, b: B): B {
    val result = b + a as B
    a : B
    return result
}
