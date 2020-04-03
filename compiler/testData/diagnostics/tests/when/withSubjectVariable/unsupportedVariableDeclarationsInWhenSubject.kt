// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun foo(): Any = 42
fun String.bar(): Any = 42


fun testSimpleValInWhenSubject() {
    when (val y = foo()) {
    }
}

fun testValWithoutInitializerWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val y: Any<!>) {
        is String -> <!DEBUG_INFO_SMARTCAST, UNINITIALIZED_VARIABLE!>y<!>.length
    }
}

fun testVarInWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>var y = foo()<!>) {
        is String -> <!DEBUG_INFO_SMARTCAST!>y<!>.length
    }
}

fun testDelegatedValInWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val y by <!UNRESOLVED_REFERENCE!>lazy<!> { 42 }<!>) {
    }
}
