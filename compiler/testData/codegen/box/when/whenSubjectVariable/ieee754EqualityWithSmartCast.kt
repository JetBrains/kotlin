// !LANGUAGE: +VariableDeclarationInWhenSubject +ProperIeee754Comparisons
// IGNORE_BACKEND: JS

val az: Any = -0.0

fun box(): String {
    when (val y = az) {
        !is Double -> throw AssertionError()
        0.0 -> {}
        else -> throw AssertionError()
    }

    return "OK"
}