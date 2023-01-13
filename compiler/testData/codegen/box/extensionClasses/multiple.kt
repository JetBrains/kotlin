// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class A(val a: String)
class B(val b: String)

context(A, B)
class C {
    fun foo() = this@A.a + this@B.b
}

fun box(): String {
    val c = with(A("O")) {
        with(B("K")) {
            C()
        }
    }
    return c.foo()
}
