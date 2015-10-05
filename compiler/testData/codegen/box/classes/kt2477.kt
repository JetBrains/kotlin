package test

interface A {
    public val c: String
        get() = "OK"
}

interface B {
    private val c: String
        get() = "FAIL"
}

abstract class C {
    abstract protected val c: String
}

open class D: C(), A, B {
    val b = c
}

fun box() : String {
    return D().c
}
