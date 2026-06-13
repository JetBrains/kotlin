// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.3,2.4
// KT-86822 is fixed no earlier than in 2.4.20-Beta1

val any = Any()
fun <T> foo(): T = any as T

fun box(): String {
    val unit1: Any = foo<Unit>()
    val unit2 = foo<Unit>()
    if (unit1 !== Unit) return "FAIL 1: $unit1"
    if (unit2 !== Unit) return "FAIL 2: $unit2"
    return "OK"
}
