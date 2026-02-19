// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun <T> eval(lambda: () -> T) = lambda()

val p = eval { "OK" }

val getter: String
    get() = eval { "OK" }

fun f() = eval { "OK" }

val obj = object : Function0<String> {
    override fun invoke() = "OK"
}

fun box(): String {
    if (p != "OK") return "FAIL"
    if (getter != "OK") return "FAIL"
    if (f() != "OK") return "FAIL"
    if (obj() != "OK") return "FAIL"

    return "OK"
}
