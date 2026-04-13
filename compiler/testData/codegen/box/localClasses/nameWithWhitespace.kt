// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// Names with whitespaces are not allowed prior to DEX version 040
// IGNORE_DEXING

fun `method with spaces`(): String {
    data class C(val s: String = "OK")
    return C().s
}

fun box(): String = `method with spaces`()
