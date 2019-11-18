// IGNORE_BACKEND_FIR: JVM_IR
val minus: Any = -0.0

fun box(): String {
    if (minus is Double) {
        if ((minus as Comparable<Double>) >= 0.0) return "fail 0"
        if ((minus as Comparable<Double>) == 0.0) return "fail 1"
        if (minus == (0.0 as Comparable<Double>)) return "fail 2"
        if (minus == (0.0F as Comparable<Float>)) return "fail 3"
    }
    return "OK"
}