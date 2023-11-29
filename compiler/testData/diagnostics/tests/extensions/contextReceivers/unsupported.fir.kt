// !DIAGNOSTICS: -UNCHECKED_CAST

<!UNSUPPORTED_FEATURE!>context(Any)<!>
fun f(g: <!UNSUPPORTED_FEATURE!>context(Any)<!> () -> Unit, value: Any): <!UNSUPPORTED_FEATURE!>context(A)<!> () -> Unit {
    return value as (<!UNSUPPORTED_FEATURE!>context(A)<!> () -> Unit)
}

fun f(g: () -> Unit, value: Any) : () -> Unit {
    return g
}

<!UNSUPPORTED_FEATURE!>context(Any)<!>
fun sameAsFWithoutNonContextualCounterpart(g: () -> Unit, value: Any) : () -> Unit {
    return g
}

<!UNSUPPORTED_FEATURE!>context(Any)<!> val p get() = 42

<!UNSUPPORTED_FEATURE!>context(String, Int)<!>
class D constructor(){}

<!UNSUPPORTED_FEATURE!>context(String, Int)<!>
class C(){}

<!UNSUPPORTED_FEATURE!>context(String, Int)<!>
class A {
    <!UNSUPPORTED_FEATURE!>context(Any)<!>
    val p: Any get() = 42

    <!UNSUPPORTED_FEATURE!>context(String, Int)<!>
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
