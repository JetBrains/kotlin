// ISSUE: KT-63508

annotation class Ann(val x: String)

fun foo() {
    class Local {
        @Ann(fun <!ANONYMOUS_FUNCTION_WITH_NAME!>f<!>(): String { return <!RETURN_TYPE_MISMATCH!>42<!> })<!SYNTAX!><!>
    }
}

@Ann(fun <!ANONYMOUS_FUNCTION_WITH_NAME!>g<!>(): String { return <!RETURN_TYPE_MISMATCH!>42<!> })<!SYNTAX!><!>
