// ISSUE: KT-74673
// IGNORE_BACKEND_K1: ANY
// ^K1 reports TYPE_MISMATCH: Type mismatch: inferred type is Foo2<P> but Foo<TypeVariable(P)> in create3
// IGNORE_BACKEND_K2: ANY

fun interface Foo<P> : suspend (P) -> Unit
fun interface Foo2<P> : suspend (P) -> Unit

class Bar<P>(foo: Foo<P>)
fun <P> create(foo: Foo2<P>): Bar<P> = Bar(foo)

fun box(): String {
    create<Int> {}
    return "OK"
}