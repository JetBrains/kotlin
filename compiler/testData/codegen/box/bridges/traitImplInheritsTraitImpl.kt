// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo(): Any = "A"
}

interface B : A {
    override fun foo(): String = "B"
}

class C : B

fun box(): String {
    val c = C()
    val b: B = c
    val a: A = c
    var r = c.foo() + b.foo() + a.foo()
    return if (r == "BBB") "OK" else "Fail: $r"
}
