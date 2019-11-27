// IGNORE_BACKEND_FIR: JVM_IR
package test

interface A {
    public val c: String
        get() = "OK"
}

interface B {
    private val c: String
        get() = "FAIL"
}

open class C {
    private val c: String = "FAIL"
}

open class D: C(), A, B {
    val b = c
}

fun box() : String {
    return D().c
}
