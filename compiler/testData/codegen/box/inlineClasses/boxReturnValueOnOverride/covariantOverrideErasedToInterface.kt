// WITH_STDLIB

interface IFoo {
    fun foo(): String
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class ICFoo(val t: IFoo): IFoo {
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