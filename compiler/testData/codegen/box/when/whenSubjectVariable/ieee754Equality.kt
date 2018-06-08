// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND: JS

val dz = -0.0

fun box(): String {
    when (val y = dz) {
        0.0 -> {}
        else -> throw AssertionError()
    }

    return "OK"
}