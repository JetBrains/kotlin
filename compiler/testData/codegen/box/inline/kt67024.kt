// Should be fixed in JS as side effect of KT-74384, in WASM as side effect of KT-74392
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR
// ^ It's passing some of js tests, and failing some other. So, completly disable JS testing here.

inline fun <reified T> id(x: T) = x

fun test1(block: (String) -> String = ::id)  = block("O")
inline fun test2(block: (String) -> String = ::id)  = block("K")

fun box() : String {
    return test1() + test2()
}