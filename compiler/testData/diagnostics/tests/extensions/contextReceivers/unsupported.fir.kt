// !DIAGNOSTICS: -UNCHECKED_CAST

context(Any)
fun f(g: context(Any) () -> Unit, value: Any): context(A) () -> Unit {
    return value as (context(A) () -> Unit)
}

fun f(g: () -> Unit, value: Any) : () -> Unit {
    return g
}

context(Any)
fun sameAsFWithoutNonContextualCounterpart(g: () -> Unit, value: Any) : () -> Unit {
    return g
}

context(Any) val p get() = 42

context(String, Int)
class A {
    context(Any)
    val p: Any get() = 42

    context(String, Int)
    fun m() {}
}

fun useWithContextReceivers() {
    with(42) {
        with("") {
            f({}, 42)
            <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>sameAsFWithoutNonContextualCounterpart<!>({}, 42)
            <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>p<!>
            val a = <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>A<!>()
            a.<!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>p<!>
            a.<!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>m<!>()
        }
    }
}
