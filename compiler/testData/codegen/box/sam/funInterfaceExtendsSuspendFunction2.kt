// ISSUE: KT-74673
// IGNORE_BACKEND_K1: ANY
// ^K1 reports TYPE_MISMATCH: Type mismatch: inferred type is Foo2<P> but Foo<TypeVariable(P)> in create3

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0 2.1.0
// ^^^ KT-74673 fixed in 2.2.0-Beta1

fun interface Foo<P> : suspend (P) -> Unit
fun interface Foo2<P> : suspend (P) -> Unit

class Bar<P>(foo: Foo<P>)
fun <P> create(foo: Foo2<P>): Bar<P> = Bar(foo)

fun box(): String {
    create<Int> {}
    return "OK"
}