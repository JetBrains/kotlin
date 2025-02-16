// ISSUE: KT-74673
// TARGET_BACKEND: JVM
// ^On JS, IMPLEMENTING_FUNCTION_INTERFACE is reported

fun interface Foo<P> : suspend (P) -> Unit
fun interface Foo2<P> : (P) -> Unit

class Bar<P>(foo: Foo<P>)
fun <P> create(foo: Foo2<P>): Bar<P> = Bar(foo)

// ##########

fun interface Foo3<T> {
    fun foo(): T
}

class Foo3Impl : Foo3<Any>, () -> String {
    override fun foo(): Any = Any()
    override fun invoke(): String = "foo"
}

class Bar3<P>(foo: Foo3<P>)

fun create3(foo3: Foo3Impl): Bar3<String> = Bar3<String>(foo3)

// ##########

fun box(): String {
    create<Int> {}
    create3(Foo3Impl())
    return "OK"
}