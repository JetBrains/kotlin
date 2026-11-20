// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=inNestedLoop.optimized.js IGNORED_MODES=USE_CONST_AND_LET_FOR_VARIABLES
// EXPECT_GENERATED_JS: function=box expect=inNestedLoop.optimized.constLet.js TARGET_MODES=USE_CONST_AND_LET_FOR_VARIABLES
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
