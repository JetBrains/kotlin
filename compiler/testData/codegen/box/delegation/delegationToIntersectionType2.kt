// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// DUMP_IR
// K1_STATUS: java.lang.AssertionError: There is still an unbound symbol after generation of IR module <main>

fun <T> select(a: T, b: T) : T = a

interface A {
    fun foo(): Any
}
interface B {
    fun foo(): String
}
class C : A, B {
    override fun foo() = "OK"
}
class D : A, B {
    override fun foo() = "FAIL"
}

fun test(c: C, d: D): String {
    val intersection = select(c, d)
    return object: A by intersection {}.foo().toString()
}

fun box() = test(C(), D())
