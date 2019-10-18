// !LANGUAGE: -ProperIeee754Comparisons
// DONT_TARGET_EXACT_BACKEND: JS_IR

fun equals1(a: Float, b: Float) = a == b

fun equals2(a: Float?, b: Float?) = a!! == b!!

fun equals3(a: Float?, b: Float?) = a != null && b != null && a == b

fun equals4(a: Float?, b: Float?) = if (a is Float && b is Float) a == b else null!!

fun equals5(a: Any?, b: Any?) = if (a is Float && b is Float) a == b else null!!


fun box(): String {
    if (-0.0F != 0.0F) return "fail 0"
    if (!equals1(-0.0F, 0.0F)) return "fail 1"
    if (!equals2(-0.0F, 0.0F)) return "fail 2"
    if (!equals3(-0.0F, 0.0F)) return "fail 3"
    if (!equals4(-0.0F, 0.0F)) return "fail 4"

    // Smart casts behavior in 1.2
    if (equals5(-0.0F, 0.0F)) return "fail 5"

    return "OK"
}

