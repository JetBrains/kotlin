// ISSUE: KT-74673
// IGNORE_BACKEND_K2: ANY

fun interface Foo<P> : suspend (P) -> Unit

class Bar<P>(foo: Foo<P>)

fun <P> create(foo: Foo<P>): Bar<P> = Bar(foo)

// ######

class FooImpl<T> : Foo<T> {
    override suspend fun invoke(p1: T) {}
}

fun <P> create2(foo: FooImpl<P>): Bar<P> = Bar(foo)

// ######

fun box(): String {
    create<Int> {}
    create2<Int>(FooImpl())
    return "OK"
}