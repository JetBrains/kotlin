fun foo() {
    val a = A()
    a.f1(f2())<caret>
}

class A {
    fun f1(): Int = 1
}

fun f2() {}

// EXISTS: f1(), f2()