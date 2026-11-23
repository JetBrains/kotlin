// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=test1;box expect=ifBranch.optimized.js TARGET_BACKENDS=JS_IR_ES6

fun test1(n: Int): String {
    var tmp: Int
    if (n > 0) {
        tmp = 23
    }
    return js("typeof tmp")
}

fun box(): String {
    var result = test1(5)
    if (result != "number") return "fail1: $result"

    result = test1(-5)
    if (result != "undefined") return "fail2: $result"

    return "OK"
}
