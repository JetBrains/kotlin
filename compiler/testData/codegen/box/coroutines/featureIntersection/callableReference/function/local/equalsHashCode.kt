// IGNORE_BACKEND: JS, NATIVE
// COMMON_COROUTINES_TEST

fun box(): String {
    suspend fun bar() {}
    suspend fun baz() {}

    if (!::bar.equals(::bar)) return "Fail 1"
    if (::bar.hashCode() != ::bar.hashCode()) return "Fail 2"

    if (::bar == ::baz) return "Fail 3"

    return "OK"
}
