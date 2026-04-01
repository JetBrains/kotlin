// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

fun `method with spaces`(): String {
    data class C(val s: String = "OK")
    return C().s
}

fun box(): String = `method with spaces`()
