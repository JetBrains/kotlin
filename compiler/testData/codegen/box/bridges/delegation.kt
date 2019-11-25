// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    fun foo(): T
}

class B : A<String> {
    override fun foo() = "OK"
}

class C(a: A<String>) : A<String> by a

fun box(): String {
    val a: A<String> = C(B())
    return a.foo()
}
