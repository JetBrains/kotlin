// LANGUAGE: +VariableDeclarationInWhenSubject
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun foo(): Any = 42
fun String.bar(): Any = 42


fun testSimpleValInWhenSubject() {
    when (val y = foo()) {
    }
}

fun testValWithoutInitializerWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val y: Any<!>) {
        <!EXPECTED_CONDITION, USELESS_IS_CHECK!>is String<!> -> <!UNINITIALIZED_VARIABLE!>y<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testVarInWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>var y = foo()<!>) {
        is String -> y.length
    }
}

fun testDelegatedValInWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>y<!> by lazy { 42 }<!>) {
    }
}
