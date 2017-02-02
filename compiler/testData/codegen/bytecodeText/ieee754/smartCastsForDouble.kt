fun equals5(a: Any?, b: Any?) = if (a is Double && b is Double?) a == b else null!!

fun equals6(a: Any?, b: Any?) = if (a is Double? && b is Double) a == b else null!!

fun equals8(a: Any?, b: Any?) = if (a is Double? && b is Double?) a == b else null!!


fun box(): String {
    if (!equals5(-0.0, 0.0)) return "fail 5"
    if (!equals6(-0.0, 0.0)) return "fail 6"

    if (!equals8(-0.0, 0.0)) return "fail 8"
    if (!equals8(null, null)) return "fail 9"
    if (equals8(null, 0.0)) return "fail 10"
    if (equals8(0.0, null)) return "fail 11"

    return "OK"
}

// 1 areEqual \(Ljava/lang/Double;Ljava/lang/Double;\)Z
// 1 areEqual \(DLjava/lang/Double;\)Z
// 1 areEqual \(Ljava/lang/Double;D\)Z
// 3 areEqual