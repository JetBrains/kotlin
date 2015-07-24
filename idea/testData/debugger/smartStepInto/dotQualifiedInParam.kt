fun foo() {
    val a = A()
    f2(a.f1())<caret>
}

class A {
    fun f1() = 1
}

fun f2(i: Int) {}

// EXISTS: f1(), f2(Int)