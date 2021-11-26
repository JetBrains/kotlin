// !SANITIZE_PARENTHESES
// IGNORE_BACKEND: JS
// !LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

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
