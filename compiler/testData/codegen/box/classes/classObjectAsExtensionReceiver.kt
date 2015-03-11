fun Any.foo() = 1

class A {
    default object
}

fun box() = if (A.foo() == 1) "OK" else "fail"
