// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS

// Exclamation marks are not valid in names in the dex file format.
// Therefore, do not attemp to dex this file as it will fail.
// See: https://source.android.com/devices/tech/dalvik/dex-format#simplename
// IGNORE_DEXING
// IGNORE_BACKEND: ANDROID

class `A!u00A0`() {
    val ok = "OK"
}

fun box(): String {
    return `A!u00A0`().ok
}