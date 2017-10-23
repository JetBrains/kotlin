// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun foo(): Any = 42
fun useInt(i: Int) {}

fun testShadowingParameter(y: Any) {
    when (val <!NAME_SHADOWING!>y<!> = foo()) {
        else -> {}
    }
}

fun testShadowedInWhenBody(x: Any) {
    when (val y = x) {
        is String -> {
            val <!NAME_SHADOWING!>y<!> = <!DEBUG_INFO_SMARTCAST!>y<!>.length
            useInt(y)
        }
    }
}

fun testShadowinLocalVariable() {
    val y = foo()
    when (val <!NAME_SHADOWING!>y<!> = foo()) {
        else -> {}
    }
}