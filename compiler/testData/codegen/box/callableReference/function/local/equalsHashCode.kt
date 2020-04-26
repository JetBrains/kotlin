// IGNORE_BACKEND: JS, JS_IR

fun box(): String {
    fun bar() {}
    fun baz() {}

    if (!::bar.equals(::bar)) return "Fail 1"
    if (::bar.hashCode() != ::bar.hashCode()) return "Fail 2"

    if (::bar == ::baz) return "Fail 3"

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS
