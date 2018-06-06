// IGNORE_BACKEND: NATIVE

inline fun ltx(a: Comparable<Any>, b: Any) = a < b
inline fun lex(a: Comparable<Any>, b: Any) = a <= b
inline fun gex(a: Comparable<Any>, b: Any) = a >= b
inline fun gtx(a: Comparable<Any>, b: Any) = a > b

inline fun lt(a: Any, b: Any) = ltx(a as Comparable<Any>, b)
inline fun le(a: Any, b: Any) = lex(a as Comparable<Any>, b)
inline fun ge(a: Any, b: Any) = gex(a as Comparable<Any>, b)
inline fun gt(a: Any, b: Any) = gtx(a as Comparable<Any>, b)

val PLUS0F = 0.0F
val MINUS0F = -0.0F
val PLUS0D = 0.0
val MINUS0D = -0.0

fun box(): String {
    return when {
        !lt(1.0F, 42.0F) -> "Fail 1 LT F"
        lt(42.0F, 1.0F) -> "Fail 2 LT F"

        !le(1.0F, 42.0F) -> "Fail 1 LE F"
        le(42.0F, 1.0F) -> "Fail 2 LE F"
        !le(1.0F, 1.0F) -> "Fail 3 LE F"

        !ge(42.0F, 1.0F) -> "Fail 1 GE F"
        ge(1.0F, 42.0F) -> "Fail 2 GE F"
        !ge(1.0F, 1.0F) -> "Fail 3 GE F"

        gt(1.0F, 42.0F) -> "Fail 1 GT F"
        !gt(42.0F, 1.0F) -> "Fail 2 GT F"

        !lt(1.0, 42.0) -> "Fail 1 LT D"
        lt(42.0, 1.0) -> "Fail 2 LT D"

        !le(1.0, 42.0) -> "Fail 1 LE D"
        le(42.0, 1.0) -> "Fail 2 LE D"
        !le(1.0, 1.0) -> "Fail 3 LE D"

        !ge(42.0, 1.0) -> "Fail 1 GE D"
        ge(1.0, 42.0) -> "Fail 2 GE D"
        !ge(1.0, 1.0) -> "Fail 3 GE D"

        gt(1.0, 42.0) -> "Fail 1 GT D"
        !gt(42.0, 1.0) -> "Fail 2 GT D"

        !lt(MINUS0F, PLUS0F) -> "Fail 1 LT +-0 F"
        lt(PLUS0F, MINUS0F) -> "Fail 2 LT +-0 F"

        !le(MINUS0F, PLUS0F) -> "Fail 1 LE +-0 F"
        le(PLUS0F, MINUS0F) -> "Fail 2 LE +-0 F"
        !le(MINUS0F, MINUS0F) -> "Fail 3 LE +-0 F"
        !le(PLUS0F, PLUS0F) -> "Fail 3 LE +-0 F"

        ge(MINUS0F, PLUS0F) -> "Fail 1 GE +-0 F"
        !ge(PLUS0F, MINUS0F) -> "Fail 2 GE +-0 F"
        !ge(MINUS0F, MINUS0F) -> "Fail 3 GE +-0 F"
        !ge(PLUS0F, PLUS0F) -> "Fail 3 GE +-0 F"

        gt(MINUS0F, PLUS0F) -> "Fail 1 GT +-0 F"
        !gt(PLUS0F, MINUS0F) -> "Fail 2 GT +-0 F"

        !lt(MINUS0D, PLUS0D) -> "Fail 1 LT +-0 D"
        lt(PLUS0D, MINUS0D) -> "Fail 2 LT +-0 D"

        !le(MINUS0D, PLUS0D) -> "Fail 1 LE +-0 D"
        le(PLUS0D, MINUS0D) -> "Fail 2 LE +-0 D"
        !le(MINUS0D, MINUS0D) -> "Fail 3 LE +-0 D"
        !le(PLUS0D, PLUS0D) -> "Fail 3 LE +-0 D"

        ge(MINUS0D, PLUS0D) -> "Fail 1 GE +-0 D"
        !ge(PLUS0D, MINUS0D) -> "Fail 2 GE +-0 D"
        !ge(MINUS0D, MINUS0D) -> "Fail 3 GE +-0 D"
        !ge(PLUS0D, PLUS0D) -> "Fail 3 GE +-0 D"

        gt(MINUS0D, PLUS0D) -> "Fail 1 GT +-0 D"
        !gt(PLUS0D, MINUS0D) -> "Fail 2 GT +-0 D"

        else -> "OK"
    }
}