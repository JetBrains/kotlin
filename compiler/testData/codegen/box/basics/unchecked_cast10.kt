// IGNORE_BACKEND: JVM, JS_IR, JS_IR_ES6, WASM

val any = Any()
fun <T> foo(): T = any as T

fun box(): String {
    val unit1: Any = foo<Unit>()
    val unit2 = foo<Unit>()
    if (unit1 !== Unit) return "FAIL 1: $unit1"
    if (unit2 !== Unit) return "FAIL 2: $unit2"
    return "OK"
}
