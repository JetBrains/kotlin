// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class X(val x: Any)

interface IFoo<T> {
    fun foo(): T
}

class TestX : IFoo<X> {
    override fun foo(): X = X("OK")
}

fun box(): String {
    val t: IFoo<X> = TestX()
    return ((t.foo() as Any) as X).x.toString()
}
