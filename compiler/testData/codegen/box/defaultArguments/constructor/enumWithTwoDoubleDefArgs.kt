enum class Foo(val a: Double = 1.0, val b: Double = 1.0) {
    A: Foo()
    B: Foo(2.0, 2.0)
    C: Foo(b = 2.0)
    D: Foo(a = 2.0)
}

fun box(): String {
    if (Foo.A.a != 1.0 || Foo.A.b != 1.0) return "fail"
    if (Foo.B.a != 2.0 || Foo.B.b != 2.0) return "fail"
    if (Foo.C.a != 1.0 || Foo.C.b != 2.0) return "fail"
    if (Foo.D.a != 2.0 || Foo.D.b != 1.0) return "fail"
    return "OK"
}
