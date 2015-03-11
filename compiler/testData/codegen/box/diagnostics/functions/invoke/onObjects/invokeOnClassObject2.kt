trait B

fun B.invoke(i: Int) = i

class A {
    default object: B {
    }
}

fun box() = if (A(42) == 42) "OK" else "fail"

