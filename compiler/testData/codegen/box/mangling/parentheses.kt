// SANITIZE_PARENTHESES
// JS error: com.google.gwt.dev.js.parserExceptions.JsParserException: missing ) after formal parameters at (95, 33)
// NATIVE error: name contains illegal characters: "()"
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// IGNORE_BACKEND: NATIVE
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

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
