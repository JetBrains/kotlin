inline fun ltx(a: Comparable<Any>, b: Any) = a < b
inline fun lex(a: Comparable<Any>, b: Any) = a <= b
inline fun gex(a: Comparable<Any>, b: Any) = a >= b
inline fun gtx(a: Comparable<Any>, b: Any) = a > b

inline fun lt(a: Any, b: Any) = ltx(a as Comparable<Any>, b)
inline fun le(a: Any, b: Any) = lex(a as Comparable<Any>, b)
inline fun ge(a: Any, b: Any) = gex(a as Comparable<Any>, b)
inline fun gt(a: Any, b: Any) = gtx(a as Comparable<Any>, b)

val ONE = 1
val ONEL = 1L

fun box(): String {
    return when {
        !lt(ONE, 42) -> "Fail 1 LT"
        lt(42, ONE) -> "Fail 2 LT"

        !le(ONE, 42) -> "Fail 1 LE"
        le(42, ONE) -> "Fail 2 LE"
        !le(1, ONE) -> "Fail 3 LE"

        !ge(42, ONE) -> "Fail 1 GE"
        ge(ONE, 42) -> "Fail 2 GE"
        !ge(1, ONE) -> "Fail 3 GE"

        gt(ONE, 42) -> "Fail 1 GT"
        !gt(42, ONE) -> "Fail 2 GT"

        !lt(ONEL, 42L) -> "Fail 1 LT L"
        lt(42L, ONEL) -> "Fail 2 LT L"

        !le(ONEL, 42L) -> "Fail 1 LE L"
        le(42L, ONEL) -> "Fail 2 LE L"
        !le(ONEL, 1L) -> "Fail 3 LE L"

        !ge(42L, ONEL) -> "Fail 1 GE L"
        ge(ONEL, 42L) -> "Fail 2 GE L"
        !ge(ONEL, 1L) -> "Fail 3 GE L"

        gt(ONEL, 42L) -> "Fail 1 GT L"
        !gt(42L, ONEL) -> "Fail 2 GT L"

        else -> "OK"
    }
}