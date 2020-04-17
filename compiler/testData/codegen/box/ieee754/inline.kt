inline fun less(a: Comparable<Double>, b: Double): Boolean {
    return a < b
}

inline fun equals(a: Comparable<Double>, b: Comparable<Double>): Boolean {
    return a == b
}

inline fun <T: Comparable<Double>> lessGeneric(a: T, b: Double): Boolean {
    return a < b
}

inline fun <T: Comparable<Double>> equalsGeneric(a: T, b: Double): Boolean {
    return a == b
}

inline fun <reified T: Comparable<Double>> lessReified(a: T, b: Double): Boolean {
    return a < b
}

inline fun <reified T: Comparable<Double>> equalsReified(a: T, b: T): Boolean {
    return a == b
}

inline fun less754(a: Double, b: Double): Boolean {
    return a < b
}

inline fun equals754(a: Double, b: Double): Boolean {
    return a == b
}

fun box(): String {
    if (!less(-0.0, 0.0)) return "fail 1"
    if (equals(-0.0, 0.0)) return "fail 2"

    if (!lessGeneric(-0.0, 0.0)) return "fail 3"
    if (equalsGeneric(-0.0, 0.0)) return "fail 4"

    if (!lessReified(-0.0, 0.0)) return "fail 5"
    if (equalsReified(-0.0, 0.0)) return "fail 6"

    if (less754(-0.0, 0.0)) return "fail 7"
    if (!equals754(-0.0, 0.0)) return "fail 8"

    return "OK"
}