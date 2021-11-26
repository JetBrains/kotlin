// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class X(val x: Any)

interface IFoo<T> {
    fun foo(): T
    fun bar(): X
}

class TestX : IFoo<X> {
    override fun foo(): X = X("O")
    override fun bar(): X = X("K")
}

fun box(): String {
    val t: IFoo<X> = TestX()
    val tFoo: Any = t.foo()
    if (tFoo !is X) {
        throw AssertionError("X expected: $tFoo")
    }

    return (t.foo() as X).x.toString() + t.bar().x.toString()
}