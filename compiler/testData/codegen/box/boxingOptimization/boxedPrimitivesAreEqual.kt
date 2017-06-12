inline fun eq(a: Any, b: Any) = a == b
inline fun ne(a: Any, b: Any) = a != b

val ONE = 1
val ONEL = 1L

fun box(): String {
    return when {
        eq(ONE, 2) -> "Fail 1"
        !eq(ONE, 1) -> "Fail 2"
        !ne(ONE, 2) -> "Fail 3"
        ne(ONE, 1) -> "Fail 4"

        eq(ONEL, 42L) -> "Fail 1L"
        !eq(ONEL, 1L) -> "Fail 2L"
        !ne(ONEL, 42L) -> "Fail 3L"
        ne(ONEL, 1L) -> "Fail 4L"

        else -> "OK"
    }
}