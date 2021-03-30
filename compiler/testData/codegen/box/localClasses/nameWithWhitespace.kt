// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JS

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
