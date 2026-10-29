// DIAGNOSTICS: -IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE
// ^^^ Kotlin/JS partially forbids implementing suspend function interfaces and report this diagnostic
// ISSUE: KT-74673

// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native,JS,Wasm-JS:2.0,2.1
// ^^^ KT-74673 fixed in 2.2.0-Beta1

fun interface Foo<P> : suspend (P) -> Unit
fun interface Foo2<P> : suspend (P) -> Unit

class Bar<P>(foo: Foo<P>)
fun <P> create(foo: Foo2<P>): Bar<P> = Bar(foo)

fun box(): String {
    create<Int> {}
    return "OK"
}
