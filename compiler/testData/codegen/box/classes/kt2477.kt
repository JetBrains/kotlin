package test

interface A {
    public val c: String
        get() = "OK"
}

interface B {
    protected val c: String
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
