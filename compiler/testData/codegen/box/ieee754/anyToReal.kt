// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val plusZero: Any = 0.0
    val minusZero: Any = -0.0
    if ((minusZero as Double) < (plusZero as Double)) return "fail 0"

    val plusZeroF: Any = 0.0F
    val minusZeroF: Any = -0.0F
    if ((minusZeroF as Float) < (plusZeroF as Float)) return "fail 1"

    if ((minusZero as Double) != (plusZero as Double)) return "fail 3"

    if ((minusZeroF as Float) != (plusZeroF as Float)) return "fail 4"

    return "OK"
}