// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=inNestedLoop.optimized.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=box expect=inNestedLoop.optimized.es6.js TARGET_BACKENDS=JS_IR_ES6
fun box(): String {
    var i: Int
    var sum = 0
    var count = 2
    while (count-- > 0) {
        i = 1
        while (true) {
            if (i >= 10) {
                break
            }
            sum += i
            i++
        }
    }

    if (sum != 90) return "fail: " + sum

    return "OK"
}
