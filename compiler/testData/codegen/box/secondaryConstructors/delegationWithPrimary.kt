// IGNORE_BACKEND_FIR: JVM_IR
internal interface A {
    fun foo(): String
}

internal class B : A {
    override fun foo() = "OK"
}

internal val global = B()

internal class C(x: Int) : A by global {
    constructor(): this(1)
}

fun box(): String {
    return C().foo()
}
