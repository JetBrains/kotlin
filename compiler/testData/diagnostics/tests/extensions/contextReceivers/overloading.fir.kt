// !LANGUAGE: +ContextReceivers

<!CONFLICTING_OVERLOADS!>context(Int, String)
fun foo(): Int<!> {
    return <!RETURN_TYPE_MISMATCH!>this<!UNRESOLVED_LABEL!>@Int<!> + 42<!>
}

<!CONFLICTING_OVERLOADS!>context(Int)
fun foo(): Int<!> {
    return <!RETURN_TYPE_MISMATCH!>this<!UNRESOLVED_LABEL!>@Int<!> + 42<!>
}

fun test() {
    with(42) {
        foo()
    }
}