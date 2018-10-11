// !LANGUAGE: +VariableDeclarationInWhenSubject +ProperIeee754Comparisons

val az: Any = -0.0
val afz: Any = -0.0f

fun box(): String {
    val y = az
    when (y) {
        !is Double -> throw AssertionError()
        0.0 -> {}
        else -> throw AssertionError()
    }
    val yy = afz
    when (yy) {
        !is Float -> throw AssertionError()
        0.0 -> {}
        else -> throw AssertionError()
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
