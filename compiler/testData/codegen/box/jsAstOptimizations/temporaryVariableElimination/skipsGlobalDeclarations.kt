// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=test;box expect=skipsGlobalDeclarations.optimized.js TARGET_BACKENDS=JS_IR_ES6

var tmp: Int = 0

fun test(a: Int, b: Int, c: Int): Int {
    tmp = a + b
    return tmp + c
}

fun box(): String {
    val result = test(2, 3, 4)
    if (result != 9) return "fail1: $result"
    if (tmp != 5) return "fail2: $tmp"

    return "OK"
}
