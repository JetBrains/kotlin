// TARGET_BACKEND: JVM_IR, WASM

annotation class A(
    val f: Float,
    val d: Double,
    val fa: FloatArray,
    val da: DoubleArray
)

fun box(): String {
    val a = A(
        Float.NaN, Double.NaN,
        floatArrayOf(Float.NaN, -0.0f, 0.0f),
        doubleArrayOf(Double.NaN, -0.0, 0.0)
    )
    val b = A(
        Float.NaN, Double.NaN,
        floatArrayOf(Float.NaN, -0.0f, 0.0f),
        doubleArrayOf(Double.NaN, -0.0, 0.0)
    )
    if (a != b) return "Fail1"

    val c = A(
        Float.NaN, Double.NaN,
        floatArrayOf(Float.NaN, 0.0f, -0.0f),
        doubleArrayOf(Double.NaN, 0.0, -0.0)
    )
    if (a == c) return "Fail2"

    if (a.hashCode() != b.hashCode()) return "Fail3"

    return "OK"
}