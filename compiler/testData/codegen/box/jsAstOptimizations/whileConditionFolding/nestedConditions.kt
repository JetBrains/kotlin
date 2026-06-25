// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=nestedConditions.optimized.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=box expect=nestedConditions.optimized.es6.js TARGET_BACKENDS=JS_IR_ES6
fun box(): String {
    var i = 1
    var sum = 0
    while (true) {
        if (i >= 5) {
            if (sum > 40) break
        }
        sum += i
        i++
    }

    if (sum != 45) return "fail: " + sum

    return "OK"
}
