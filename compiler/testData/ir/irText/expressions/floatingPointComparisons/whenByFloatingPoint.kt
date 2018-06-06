fun testSimple(x: Double) =
        when (x) {
            0.0 -> 0
            else -> 1
        }

fun testSmartCastInWhenSubject(x: Any): Int {
    if (x !is Double) return -1
    return when (x) {
        0.0 -> 0
        else -> 1
    }
}

fun testSmartCastInWhenCondition(x: Double, y: Any): Int {
    if (y !is Double) return -1
    return when (x) {
        y -> 0
        else -> 1
    }
}

fun testSmartCastInWhenConditionInBranch(x: Any) =
    when (x) {
        !is Double -> -1
        0.0 -> 0
        else -> 1
    }

fun testSmartCastToDifferentTypes(x: Any, y: Any): Int {
    if (x !is Double) return -1
    if (y !is Float) return -1
    return when (x) {
        y -> 0
        else -> 1
    }
}

fun foo(x: Double) = x

fun testWithPrematureExitInConditionSubexpression(x: Any): Int {
    return when (x) {
        foo(
            if (x !is Double) return 42 else x
        ) -> 0
        else -> 1
    }
}