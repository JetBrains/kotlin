// LANGUAGE: +VariableDeclarationInWhenSubject
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun foo(): Any = 42
fun useInt(i: Int) {}

fun testShadowingParameter(y: Any) {
    when (val y = foo()) {
        else -> {}
    }
}

fun testShadowedInWhenBody(x: Any) {
    when (val y = x) {
        is String -> {
            val y = y.length
            useInt(y)
        }
    }
}

fun testShadowinLocalVariable() {
    val y = foo()
    when (val y = foo()) {
        else -> {}
    }
}