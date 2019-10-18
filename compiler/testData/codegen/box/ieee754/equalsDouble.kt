// !LANGUAGE: -ProperIeee754Comparisons
// DONT_TARGET_EXACT_BACKEND: JS_IR

fun equals1(a: Double, b: Double) = a == b

fun equals2(a: Double?, b: Double?) = a!! == b!!

fun equals3(a: Double?, b: Double?) = a != null && b != null && a == b

fun equals4(a: Double?, b: Double?) = if (a is Double && b is Double) a == b else null!!

fun equals5(a: Any?, b: Any?) = if (a is Double && b is Double) a == b else null!!


fun box(): String {
    if (-0.0 != 0.0) return "fail 0"
    if (!equals1(-0.0, 0.0)) return "fail 1"
    if (!equals2(-0.0, 0.0)) return "fail 2"
    if (!equals3(-0.0, 0.0)) return "fail 3"
    if (!equals4(-0.0, 0.0)) return "fail 4"

    // Smart casts behavior in 1.2
    if (equals5(-0.0, 0.0)) return "fail 5"

    return "OK"
}

