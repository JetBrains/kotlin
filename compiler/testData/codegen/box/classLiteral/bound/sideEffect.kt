// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

fun box(): String {
    var x = 42

    val k1 = (x++)::class
    if (k1 != Int::class) return "Fail 1: $k1"
    if (x != 43) return "Fail 2: $x"

    val k2 = { x *= 2; x }()::class
    // Note that k2 is the class of the wrapper type java.lang.Integer
    if (k2 != Integer::class) return "Fail 3: $k2"
    if (x != 86) return "Fail 4: $x"

    return "OK"
}
