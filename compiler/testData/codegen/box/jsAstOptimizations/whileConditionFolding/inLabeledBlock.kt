// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=inLabeledBlock.optimized.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=box expect=inLabeledBlock.optimized.es6.js TARGET_BACKENDS=JS_IR_ES6
fun box(): String {
    var i = 1
    var sum = 0
    outer@ do {
        while (true) {
            if (i >= 10) {
                break;
            }
            sum += i
            i++
            if (sum > 20) break@outer
        }
    } while (false)

    if (sum != 21) return "fail: " + sum

    return "OK"
}
