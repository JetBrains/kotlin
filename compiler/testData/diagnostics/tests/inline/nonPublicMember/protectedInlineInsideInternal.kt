// FIR_IDENTICAL
// ISSUE: KT-58757

internal abstract class Foo {
    abstract val context: CharSequence
}

internal abstract class Bar(protected val foo: Foo) {
    protected inline val inlineContext: String
        get() = foo.context as String
}
