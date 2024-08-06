// IGNORE_BACKEND_K2: JVM_IR
// ISSUE: KT-70417

interface A {
    var x: String
}

open class B : A {
    override lateinit var x: String
}

interface C: A

open class D : C  {
    override var x: String
        get() = "OK"
        set(_) {}
}

class E : B(), C by D()

fun box(): String {
    val e = E()
    e.x = "Fail"
    return e.x
}