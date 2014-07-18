trait B

fun B.invoke(i: Int) = i

class A {
    class object: B {
    }
}

fun box() = if (A(42) == 42) "OK" else "fail"

