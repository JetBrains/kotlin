// WITH_STDLIB
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.3
// ^^^ KT-15101 js: Same callable references are not equal

fun box(): String {
    suspend fun bar() {}
    suspend fun baz() {}

    if (!::bar.equals(::bar)) return "Fail 1"
    if (::bar.hashCode() != ::bar.hashCode()) return "Fail 2"

    if (::bar == ::baz) return "Fail 3"

    return "OK"
}
