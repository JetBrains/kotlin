// !LANGUAGE: +VariableDeclarationInWhenSubject

fun foo(): Any = 42

fun test() {
    when (val x = foo()) {
    }
}