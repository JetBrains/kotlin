// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

interface IFoo {
    fun foo(): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC: IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICFoo(val t: IFoo): IC() {
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