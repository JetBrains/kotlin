// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    open val baz: String
        get() = "T.baz"
}

open class A {
    open val bar: String
        get() = "OK"
    open val boo: String
        get() = "OK"
}

open class B : A(), T {
    override val bar: String
        get() = "B"
    override val baz: String
        get() = "B.baz"
    inner class E {
        val bar: String
            get() = super<A>@B.bar + super@B.bar + super@B.baz
    }
}

class C : B() {
    override val bar: String
        get() = "C"
    override val boo: String
        get() = "C"
    inner class D {
        val bar: String
            get() = super<B>@C.bar + super<B>@C.boo
    }
}

fun box(): String {
    var r = ""

    r = B().E().bar
    if (r != "OKOKT.baz") return "fail 1; r = $r"

    r = C().D().bar
    if (r != "BOK") return "fail 2; r = $r"

    return "OK"
}
