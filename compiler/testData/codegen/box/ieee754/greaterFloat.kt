// !LANGUAGE: -ProperIeee754Comparisons
// IGNORE_BACKEND_FIR: JVM_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR

fun greater1(a: Float, b: Float) = a > b

fun greater2(a: Float?, b: Float?) = a!! > b!!

fun greater3(a: Float?, b: Float?) = a != null && b != null && a > b

fun greater4(a: Float?, b: Float?) = if (a is Float && b is Float) a > b else null!!

fun greater5(a: Any?, b: Any?) = if (a is Float && b is Float) a > b else null!!

fun box(): String {
    if (0.0F > -0.0F) return "fail 0"
    if (greater1(0.0F, -0.0F)) return "fail 1"
    if (greater2(0.0F, -0.0F)) return "fail 2"
    if (greater3(0.0F, -0.0F)) return "fail 3"
    if (greater4(0.0F, -0.0F)) return "fail 4"

    // Smart casts behavior in 1.2
    if (!greater5(0.0F, -0.0F)) return "fail 5"

    return "OK"
}