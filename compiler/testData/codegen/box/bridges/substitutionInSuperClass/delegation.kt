// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    fun id(t: T): T
}

open class B : A<String> {
    override fun id(t: String) = t
}

class C : B()

class D : A<String> by C()

fun box(): String {
    val d = D()
    if (d.id("") != "") return "Fail"
    val a: A<String> = d
    return a.id("OK")
}
