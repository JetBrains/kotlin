// !DIAGNOSTICS: -UNUSED_VARIABLE

fun testWhen(x: Any?) {
    val y = <!INAPPLICABLE_CANDIDATE!>when (x) {
        null -> ""
        else -> ::<!UNRESOLVED_REFERENCE!>unresolved<!>
    }<!>
}

fun testWhenWithBraces(x: Any?) {
    val z = <!INAPPLICABLE_CANDIDATE!>when(x) {
        null -> { "" }
        else -> { ::<!UNRESOLVED_REFERENCE!>unresolved<!> }
    }<!>
}

fun testIf(x: Any?) {
    val y = <!INAPPLICABLE_CANDIDATE!>if (x != null) ::<!UNRESOLVED_REFERENCE!>unresolved<!> else null<!>
}

fun testIfWithBraces(x: Any?) {
    val z = <!INAPPLICABLE_CANDIDATE!>if (x != null) { ::<!UNRESOLVED_REFERENCE!>unresolved<!> } else { null }<!>
}

fun testElvis(x: Any?) {
    val y = x <!INAPPLICABLE_CANDIDATE!>?:<!> ::<!UNRESOLVED_REFERENCE!>unresolved<!>
}

fun testExclExcl() {
    val y = :: <!UNRESOLVED_REFERENCE!>unresolved<!><!INAPPLICABLE_CANDIDATE, NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
}

fun testTry() {
    val v = <!INAPPLICABLE_CANDIDATE!>try { ::<!UNRESOLVED_REFERENCE!>unresolved<!> } catch (e: Exception) {}<!>
}
