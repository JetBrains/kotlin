// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND: JS, JS_IR

val dz = -0.0
val fz = -0.0f

fun box(): String {
    when (val y = dz) {
        0.0 -> {}
        else -> throw AssertionError()
    }

    when (val y = dz) {
        else -> {
            if (y < 0.0) throw AssertionError()
            if (y > 0.0) throw AssertionError()
        }
    }

    when (val y = fz) {
        0.0f -> {}
        else -> throw AssertionError()
    }

    when (val y = fz) {
        else -> {
            if (y < 0.0f) throw AssertionError()
            if (y > 0.0f) throw AssertionError()
        }
    }

    return "OK"
}