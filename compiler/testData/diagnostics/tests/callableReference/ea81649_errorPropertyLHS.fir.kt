// Different modules are important for this test because otherwise everything is analyzed at once and some errors
// already exist in the binding context when we're analyzing "User::surname".
// (The assertion at DoubleColonExpressionResolver.checkNoExpressionOnLHS is only performed when there are no errors in the binding context)

// MODULE: m1
// FILE: bar.kt

fun <T> bar(ff: <!UNRESOLVED_REFERENCE!>Err<!>.() -> Unit) {
}

// MODULE: m2(m1)
// FILE: foo.kt

data class User(val surname: String)

fun foo() {
    bar<String> <!UNRESOLVED_REFERENCE!>{
        User::surname
    }<!>
}
