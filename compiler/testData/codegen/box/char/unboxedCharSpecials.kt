// TARGET_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: lib.kt
inline fun typeOf(x: dynamic): String = js("typeof x").unsafeCast<String>()

// FILE: main.kt
fun box(): String {
    val arr = charArrayOf('A')

    var r = typeOf(arr.iterator().asDynamic().nextChar())
    if (r != "number") return "fail1: $r"
    r = typeOf(arr.iterator().asDynamic().next())
    if (r != "object") return "fail1a: $r"

    var progression = 'A'..'Z'
    r = typeOf(progression.asDynamic().first)
    if (r != "number") return "fail2: $r"
    r = typeOf(progression.asDynamic().last)
    if (r != "number") return "fail3: $r"

    r = typeOf(Char.asDynamic().MIN_HIGH_SURROGATE)
    if (r != "number") return "fail4: $r"

    return "OK"
}

fun getInt() = 65