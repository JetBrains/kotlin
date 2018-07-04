// !LANGUAGE: +VariableDeclarationInWhenSubject +ProperIeee754Comparisons
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS, JS_IR

val az: Any = -0.0
val afz: Any = -0.0f

fun box(): String {
    when (val y = az) {
        !is Double -> throw AssertionError()
        0.0 -> {}
        else -> throw AssertionError()
    }

    when (val y = afz) {
        !is Float -> throw AssertionError()
        0.0 -> {}
        else -> throw AssertionError()
    }

    return "OK"
}