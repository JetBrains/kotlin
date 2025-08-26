
fun processNumber(number: Number): Number = number

fun box(): String {
    val double: Double? = 0.0
    return if (processNumber(double ?: 0) == 0.0) "OK" else "FAIL"
}