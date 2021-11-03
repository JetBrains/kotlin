// !DIAGNOSTICS: -UNCHECKED_CAST

<!UNSUPPORTED_FEATURE!>context(Any)<!>
fun f(g: <!UNSUPPORTED_FEATURE!>context(Any)<!> () -> Unit, value: Any): <!UNSUPPORTED_FEATURE!>context(A)<!> () -> Unit {
    return value as (<!UNSUPPORTED_FEATURE!>context(A)<!> () -> Unit)
}

<!UNSUPPORTED_FEATURE!>context(String, Int)<!>
class A {
    <!UNSUPPORTED_FEATURE!>context(Any)<!>
    val p: Any get() = 42

    <!UNSUPPORTED_FEATURE!>context(String, Int)<!>
    fun m() {}
}