// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// Names with spaces are not valid according to the dex specification
// before DEX version 040. Therefore, do not attempt to dex the resulting
// class file with a min api below 30.
//
// See: https://source.android.com/devices/tech/dalvik/dex-format#simplename
// IGNORE_BACKEND: ANDROID

fun `method with spaces`(): String {
    data class C(val s: String = "OK")
    return C().s
}

fun box(): String = `method with spaces`()
