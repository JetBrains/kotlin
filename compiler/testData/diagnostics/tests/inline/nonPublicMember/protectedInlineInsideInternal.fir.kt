// ISSUE: KT-58757

internal abstract class Foo {
    abstract val context: CharSequence
}

internal abstract class Bar(protected val foo: Foo) {
    protected inline val inlineContext: String
        get() = <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>context<!> as String
}
