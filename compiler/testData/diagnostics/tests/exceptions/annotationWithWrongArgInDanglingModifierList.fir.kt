// ISSUE: KT-63508

annotation class Ann(val x: String)

fun foo() {
    class Local {
        @Ann(<!ANONYMOUS_FUNCTION_WITH_NAME!>fun f(): String { return <!RETURN_TYPE_MISMATCH!>42<!> }<!>)<!SYNTAX!><!>
    }
}

@Ann(<!ANONYMOUS_FUNCTION_WITH_NAME!>fun g(): String { return <!RETURN_TYPE_MISMATCH!>42<!> }<!>)<!SYNTAX!><!>
