// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=foo;box expect=assignmentToOuterVar.optimized.js TARGET_BACKENDS=JS_IR_ES6

var value = "OK"

fun foo(newValue: String): String {
    var tmp = value
    value = newValue
    return tmp
}

fun box(): String {
    return foo("fail")
}
