// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box(): String {
    val f = { }
    val class1 = (Runnable(f) as Object).getClass()
    val class2 = (Runnable(f) as Object).getClass()

    return if (class1 == class2) "OK" else "$class1 $class2"
}
