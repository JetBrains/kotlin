// Different modules are important for this test because otherwise everything is analyzed at once and some errors
// already exist in the binding context when we're analyzing "User::surname".
// (The assertion at DoubleColonExpressionResolver.checkNoExpressionOnLHS is only performed when there are no errors in the binding context)

// MODULE: m1
// FILE: bar.kt

fun <T> bar(<!UNUSED_PARAMETER!>ff<!>: <!UNRESOLVED_REFERENCE!>Err<!>.() -> Unit) {
}

// MODULE: m2(m1)
// FILE: foo.kt

data class User(val surname: String)

fun foo() {
    bar<String> {
        <!UNUSED_EXPRESSION!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>User<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>surname<!><!>
    }
}
