// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VALUE

fun foo(): Any = 42

fun test(x: Any) {
    when (val y = foo()) {
        is String -> <!VAL_REASSIGNMENT!>y<!> = ""
    }
}
