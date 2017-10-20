// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun foo(): Any = 42

fun test(y: Any) {
    val z = when (val <!NAME_SHADOWING!>y<!> = foo()) {
        42 -> "Magic: $y"
        else -> "Not magic: $y"
    }
}