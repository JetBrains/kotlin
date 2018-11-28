// !LANGUAGE: +VariableDeclarationInWhenSubject

fun foo(): Any = 42

fun test() {
    when (val <!UNUSED_VARIABLE!>x<!> = foo()) {
    }
}