interface B

operator fun B.invoke(i: Int) = i

class A {
    companion object: B {
    }
}

fun box() = if (A(42) == 42) "OK" else "fail"

