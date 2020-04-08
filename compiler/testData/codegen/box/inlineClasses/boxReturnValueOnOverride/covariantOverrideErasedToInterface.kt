// IGNORE_BACKEND_FIR: JVM_IR

interface IFoo {
    fun foo(): String
}

inline class ICFoo(val t: IFoo): IFoo {
    override fun foo(): String = t.foo()
}

interface IBar {
    fun bar(): IFoo
}

object FooOK : IFoo {
    override fun foo(): String = "OK"
}

class Test : IBar {
    override fun bar(): ICFoo = ICFoo(FooOK)
}

fun box(): String {
    val test: IBar = Test()
    val bar = test.bar()
    if (bar !is ICFoo) {
        throw AssertionError("bar: $bar")
    }
    return bar.foo()
}