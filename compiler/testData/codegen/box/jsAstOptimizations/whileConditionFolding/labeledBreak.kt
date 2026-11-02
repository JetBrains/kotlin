// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=labeledBreak.optimized.js IGNORED_MODES=USE_CONST_AND_LET_FOR_VARIABLES
// EXPECT_GENERATED_JS: function=box expect=labeledBreak.optimized.constLet.js TARGET_MODES=USE_CONST_AND_LET_FOR_VARIABLES
fun box(): String {
    var i = 0
    var j = 0
    var log = ""
    outer@ while (i < 10) {
        while (j < 10) {
            if (i + j >= 3) {
                break@outer
            }
            log += "$i,$j;"
            j++
        }
        i++
    }

    if (log != "0,0;0,1;0,2;") return "fail: " + log

    return "OK"
}
