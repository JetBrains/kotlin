// !SANITIZE_PARENTHESES
// IGNORE_BACKEND: JS, JS_IR

// Sanitization is needed here because DxChecker reports ParseException on parentheses in names.

class `()` {
    fun `()`(): String {
        fun foo(): String {
            return bar { baz() }
        }
        return foo()
    }

    fun baz() = "OK"
}

fun bar(p: () -> String) = p()

fun box(): String {
    return `()`().`()`()
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS
