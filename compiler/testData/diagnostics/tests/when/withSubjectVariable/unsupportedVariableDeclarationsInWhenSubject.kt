// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// WITH_RUNTIME

data class P(val x: Any, val y: Any)

fun foo(): Any = 42
fun String.bar(): Any = 42


fun testSimpleValInWhenSubject() {
    when (val y = foo()) {
    }
}

fun testValWithoutInitializerWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val y: Any<!>) {
        is String -> <!UNINITIALIZED_VARIABLE!>y<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testVarInWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>var y = foo()<!>) {
        is String -> y.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testDestructuringInWhenSubject() {
    when (val (y1, y2) = P(1, 2)) {
    }
}

fun testExtensionValInWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val <!LOCAL_EXTENSION_PROPERTY, DEBUG_INFO_MISSING_UNRESOLVED!>String<!>.<!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>len<!><!><!SYNTAX!><!> <!UNRESOLVED_REFERENCE!>get<!>(<!SYNTAX!><!>) = <!UNRESOLVED_REFERENCE!>bar<!>()<!SYNTAX!>)<!> <!UNUSED_LAMBDA_EXPRESSION!>{
    }<!>
}

fun testDelegatedValInWhenSubject() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val y by <!UNRESOLVED_REFERENCE!>lazy<!> { 42 }<!>) {
    }
}
