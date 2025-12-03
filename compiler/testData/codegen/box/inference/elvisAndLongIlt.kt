// ISSUE: KT-81763
// LANGUAGE: +DontUseConstraintFromEqualityOperatorInElvis

fun box(): String {
    val v1: Long? = null
    val v2: Long? = null
    val v3: Boolean = (v1 ?: 0) != (v2 ?: 0)
    if (v3) return "FAIL 1"
    if (!bar(1, 1)) return "FAIL 2"
    if (bar(1, null)) return "FAIL 3"
    if (!bar(0, null)) return "FAIL 4"

    return "OK"
}

fun bar(x: Long, y: Long?): Boolean = (x == (y ?: 0))
