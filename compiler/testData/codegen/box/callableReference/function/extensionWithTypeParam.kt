class A {
    var result1 = "Fail"
}

class B <T> {
    var result2 = "Fail"
}

fun <T> A.foo(a: T) {
    result1 = "O"
}

fun <T> B<T>.foo(a: T) {
    result2 = "K"
}

fun box(): String {
    val a = A()
    val fun1 : A.(Int) -> Unit = A::foo
    a.fun1(1)

    val b = B<Int>()
    val fun2 : (Int) -> Unit = b::foo
    fun2(1)
    return a.result1 + b.result2
}
