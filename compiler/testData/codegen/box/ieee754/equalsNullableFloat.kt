// !LANGUAGE: -ProperIeee754Comparisons
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR

fun equals1(a: Float, b: Float?) = a == b

fun equals2(a: Float?, b: Float?) = a!! == b!!

fun equals3(a: Float?, b: Float?) = a != null && a == b

fun equals4(a: Float?, b: Float?) = if (a is Float) a == b else null!!

fun equals5(a: Any?, b: Any?) = if (a is Float && b is Float?) a == b else null!!

fun equals6(a: Any?, b: Any?) = if (a is Float? && b is Float) a == b else null!!

fun equals7(a: Float?, b: Float?) = a == b

fun equals8(a: Any?, b: Any?) = if (a is Float? && b is Float?) a == b else null!!


fun box(): String {
    if (!equals1(-0.0F, 0.0F)) return "fail 1"
    if (!equals2(-0.0F, 0.0F)) return "fail 2"
    if (!equals3(-0.0F, 0.0F)) return "fail 3"
    if (!equals4(-0.0F, 0.0F)) return "fail 4"

    // Smart casts behavior in 1.2
    if (equals5(-0.0F, 0.0F)) return "fail 5"
    if (equals6(-0.0F, 0.0F)) return "fail 6"

    if (!equals7(-0.0F, 0.0F)) return "fail 7"

    // Smart casts behavior in 1.2
    if (equals8(-0.0F, 0.0F)) return "fail 8"

    if (!equals8(null, null)) return "fail 9"
    if (equals8(null, 0.0F)) return "fail 10"
    if (equals8(0.0F, null)) return "fail 11"

    return "OK"
}

