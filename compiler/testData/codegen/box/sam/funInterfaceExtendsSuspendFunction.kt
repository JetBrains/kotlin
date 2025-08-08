// ISSUE: KT-74673

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0 2.1.0
// ^^^ KT-74673 fixed in 2.2.0-Beta1

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