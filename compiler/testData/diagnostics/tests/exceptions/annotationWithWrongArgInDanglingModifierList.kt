// ISSUE: KT-63508

annotation class Ann(val x: String)

fun foo() {
    class Local {
        @Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>fun <!ANONYMOUS_FUNCTION_WITH_NAME!>f<!>(): String { return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!> }<!>)<!SYNTAX!><!>
    }
}

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>fun <!ANONYMOUS_FUNCTION_WITH_NAME!>g<!>(): String { return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!> }<!>)<!SYNTAX!><!>
