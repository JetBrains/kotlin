// TARGET_BACKEND: WASM

// Type of functions `(T) -> T` are rendered the same way in IrType::render
fun <T : JsAny?> foo(f: (T) -> T, x: Int): Int = js("f(x)")
fun <T : JsAny?> foo(f: (T) -> T, x: String): String = js("f(x)")

fun box(): String {
    if (foo<JsAny>({ it }, 10) != 10) return "Fail 1"
    if (foo<JsAny>({ it }, "20") != "20") return "Fail 2"
    return "OK"
}