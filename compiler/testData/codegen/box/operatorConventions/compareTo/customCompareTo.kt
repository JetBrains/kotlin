// IGNORE_BACKEND_FIR: JVM_IR

var longCompareToInvocations = 0
var doubleCompareToInvocations = 0

private operator fun Long?.compareTo(other: Long?): Int {
    longCompareToInvocations++
    val diff = (this ?: 0L) - (other ?: 0L)
    return when {
        diff < 0L -> -1
        diff > 0L -> 1
        else -> 0
    }
}

private operator fun Double?.compareTo(other: Double?): Int {
    doubleCompareToInvocations++
    val diff = (this ?: 0.0) - (other ?: 0.0)
    return when {
        diff < 0.0 -> -1
        diff > 0.0 -> 1
        else -> 0
    }
}

fun checkLong(): String? {
    val a: Long? = null
    val b: Long? = 42L

    if (a > b) return "Fail Long >"
    if (a >= b) return "Fail Long >="
    if (!(a < b)) return "Fail Long <"
    if (!(a <= b)) return "Fail Long <="

    if (longCompareToInvocations != 4) return "Fail: expected 4 compareTo invocations, but was $longCompareToInvocations"
    
    return null
}

fun checkDouble(): String? {
    val a: Double? = null
    val b: Double? = 3.14

    if (a > b) return "Fail Double >"
    if (a >= b) return "Fail Double >="
    if (!(a < b)) return "Fail Double <"
    if (!(a <= b)) return "Fail Double <="

    if (doubleCompareToInvocations != 4) return "Fail: expected 4 compareTo invocations, but was $doubleCompareToInvocations"

    return null
}

fun box(): String {
    checkLong()?.let { return it }
    checkDouble()?.let { return it }
    return "OK"
}
