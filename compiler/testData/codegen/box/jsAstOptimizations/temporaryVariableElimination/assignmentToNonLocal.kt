// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=assignmentToNonLocal.optimized.js TARGET_BACKENDS=JS_IR_ES6

var result = ""

fun box(): String {
    val tmp: String
    tmp = result
    result += "fail"
    result = tmp + "OK"
    return result
}
