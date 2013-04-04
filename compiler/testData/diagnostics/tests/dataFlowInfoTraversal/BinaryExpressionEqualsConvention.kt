trait A
trait B : A

fun foo1(a: A, b: B): Boolean {
    val result = a as B == b
    a : B
    return result
}

fun foo2(a: A, b: B): Boolean {
    val result = b == a as B
    a : B
    return result
}
