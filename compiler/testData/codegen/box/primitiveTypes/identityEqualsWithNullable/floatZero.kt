// TARGET_BACKEND: JVM_IR

fun box(): String {
    val zero: Float = 0.0f
    val minusZero: Float = -0.0f
    val boxedZero: Float? = 0.0f
    val boxedMinusZero: Float? = -0.0f

    if (!(zero === minusZero)) return "Fail zero === minusZero"
    if (zero !== minusZero) return "Fail zero !== minusZero"

    if (!(minusZero === zero)) return "Fail minusZero === zero"
    if (minusZero !== zero) return "Fail minusZero !== zero"

    if (zero === boxedZero) return "Fail zero === boxedZero"
    if (!(zero !== boxedZero)) return "Fail zero !== boxedZero"

    if (zero === boxedMinusZero) return "Fail zero === boxedMinusZero"
    if (!(zero !== boxedMinusZero)) return "Fail zero !== boxedMinusZero"

    if (minusZero === boxedZero) return "Fail minusZero === boxedZero"
    if (!(minusZero !== boxedZero)) return "Fail minusZero !== boxedZero"

    if (minusZero === boxedMinusZero) return "Fail minusZero === boxedMinusZero"
    if (!(minusZero !== boxedMinusZero)) return "Fail minusZero !== boxedMinusZero"

    return "OK"
}
