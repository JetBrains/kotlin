// DUMP_IR
// FIR_IDENTICAL

abstract class A {
    abstract val x: String
}

interface B {
    var x: String
}

abstract class C : A(), B

class D(override var x: String) : C()

fun test(c: C) {
    c.x = "OK"
}

fun box(): String {
    val d = D("Fail")
    test(d)
    return d.x
}
