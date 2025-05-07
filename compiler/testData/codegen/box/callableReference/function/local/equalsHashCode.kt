// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun box(): String {
    fun bar() {}
    fun baz() {}

    if (!::bar.equals(::bar)) return "Fail 1"
    if (::bar.hashCode() != ::bar.hashCode()) return "Fail 2"

    if (::bar == ::baz) return "Fail 3"

    return "OK"
}
