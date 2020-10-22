// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE
// not sure if it's ok to change Object to Any

// WITH_RUNTIME

package test.regressions.kt1172

public fun scheduleRefresh(vararg files : Object) {
    ArrayList<Object>(files.map { it })
}

fun box(): String {
    scheduleRefresh()
    return "OK"
}
