// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class X(val x: String)

interface IFoo1<T> {
    fun foo(x: T): X
}

interface IFoo2 {
    fun foo(x: String): X
}

class Test : IFoo1<String>, IFoo2 {
    override fun foo(x: String): X = X(x)
}

fun box(): String {
    val t1: IFoo1<String> = Test()
    val t2: IFoo2 = Test()
    return t1.foo("O").x + t2.foo("K").x
}