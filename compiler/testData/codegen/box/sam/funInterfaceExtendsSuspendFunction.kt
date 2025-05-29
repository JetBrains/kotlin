// ISSUE: KT-74673
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ JS target doesn't support both function and suspend function types as supertypes

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