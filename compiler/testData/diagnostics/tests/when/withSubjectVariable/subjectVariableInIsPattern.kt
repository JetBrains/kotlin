// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun test(x: Any) {
    when (val y = x) {
        is String -> {}
    }
}