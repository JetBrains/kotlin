trait A {
    fun foo(): String
}

class B : A {
    override fun foo() = "OK"
}

val global = B()

class C(x: Int) : A by global {
    constructor(): this(1) {}
}

fun box(): String {
    return C().foo()
}
