// !LANGUAGE: +ReleaseCoroutines
// !API_VERSION: 1.3
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE

fun box(): String {
    suspend fun bar() {}
    suspend fun baz() {}

    if (!::bar.equals(::bar)) return "Fail 1"
    if (::bar.hashCode() != ::bar.hashCode()) return "Fail 2"

    if (::bar == ::baz) return "Fail 3"

    return "OK"
}
