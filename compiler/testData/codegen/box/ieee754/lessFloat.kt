// !LANGUAGE: -ProperIeee754Comparisons
// IGNORE_BACKEND_FIR: JVM_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR

fun less1(a: Float, b: Float) = a < b

fun less2(a: Float?, b: Float?) = a!! < b!!

fun less3(a: Float?, b: Float?) = a != null && b != null && a < b

fun less4(a: Float?, b: Float?) = if (a is Float && b is Float) a < b else true

fun less5(a: Any?, b: Any?) = if (a is Float && b is Float) a < b else true

fun box(): String {
    if (-0.0F < 0.0F) return "fail 0"
    if (less1(-0.0F, 0.0F)) return "fail 1"
    if (less2(-0.0F, 0.0F)) return "fail 2"
    if (less3(-0.0F, 0.0F)) return "fail 3"
    if (less4(-0.0F, 0.0F)) return "fail 4"

    // Smart casts behavior in 1.2
    if (!less5(-0.0F, 0.0F)) return "fail 5"

    return "OK"
}