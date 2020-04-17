// !LANGUAGE: +VariableDeclarationInWhenSubject

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

    testDoubleAsUpperBound(-0.0)

    return "OK"
}

fun <T: Double> testDoubleAsUpperBound(v: T): Boolean {
    return when (val a = v*v) {
        0.0 -> true
        else -> throw AssertionError()
    }
}
