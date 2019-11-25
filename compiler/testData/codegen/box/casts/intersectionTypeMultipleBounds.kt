// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo(): Any?
    fun bar(): String
}

interface B {
    fun foo(): String
}

fun <T> bar(x: T): String where T : A, T : B {
    if (x.foo().length != 2 || x.foo() != "OK") return "fail 1"
    if (x.bar() != "ok") return "fail 2"

    return "OK"
}

class C : A, B {
    override fun foo() = "OK"
    override fun bar() = "ok"
}

fun box(): String = bar(C())