// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// WITH_RUNTIME

fun foo(): Any = 42
fun String.bar(): Any = 42


fun testSimpleValInWhenSubject() {
    when (val y = foo()) {
    }
}

fun testValWithoutInitializerWhenSubject() {
    when (val y: Any) {
        is String -> y.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testVarInWhenSubject() {
    when (var y = foo()) {
        is String -> y.length
    }
}

fun testDelegatedValInWhenSubject() {
    when (val y by lazy { 42 }) {
    }
}
