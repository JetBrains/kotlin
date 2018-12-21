// !API_VERSION: 1.0
// IGNORE_BACKEND: JVM_IR

fun equals5(a: Any?, b: Any?) = if (a is Float && b is Float?) a == b else null!!

fun equals6(a: Any?, b: Any?) = if (a is Float? && b is Float) a == b else null!!

fun equals8(a: Any?, b: Any?) = if (a is Float? && b is Float?) a == b else null!!


fun box(): String {
    if (!equals5(-0.0F, 0.0F)) return "fail 5"
    if (!equals6(-0.0F, 0.0F)) return "fail 6"

    if (!equals8(-0.0F, 0.0F)) return "fail 8"
    if (!equals8(null, null)) return "fail 9"
    if (equals8(null, 0.0F)) return "fail 10"
    if (equals8(0.0F, null)) return "fail 11"

    return "OK"
}

// 3 areEqual \(Ljava/lang/Object;Ljava/lang/Object;\)Z
// 3 areEqual
