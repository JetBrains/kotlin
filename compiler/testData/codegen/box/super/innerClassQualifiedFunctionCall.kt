interface T {
    open fun baz(): String = "T.baz"
}

open class A {
    open val foo: String = "OK"
    open fun bar(): String = "OK"
    open fun boo(): String = "OK"
}

open class B : A(), T {
    override fun bar(): String = "B"
    override fun baz(): String = "B.baz"
    inner class E {
        val foo: String = super<A>@B.foo
        fun bar() = super<A>@B.bar() + super@B.bar() + super@B.baz()
    }
}

class C : B() {
    override fun bar(): String = "C"
    override fun boo(): String = "C"
    inner class D {
        val foo: String = super<B>@C.foo
        fun bar() = super<B>@C.bar() + super<B>@C.boo()
    }
}

fun box(): String {
    var r = ""

    r = B().E().foo
    if (r != "OK") return "fail 1; r = $r"
    r = ""
    r = B().E().bar()
    if (r != "OKOKT.baz") return "fail 2; r = $r"

    r = ""
    r = C().D().foo
    if (r != "OK") return "fail 3; r = $r"
    r = ""
    r = C().D().bar()
    if (r != "BOK") return "fail 4; r = $r"

    return "OK"
}
