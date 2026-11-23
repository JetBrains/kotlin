// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=foo;bar;box expect=tryCatch.optimized.js TARGET_BACKENDS=JS_IR_ES6

fun foo(): Int {
    throw Exception("foo")
    return -1
}

fun bar(): String {
    val tmp = foo()
    try {
        return "result: $tmp"
    } catch (e: Exception) {
        return "error"
    }
}

fun box(): String {
    try {
        bar()
    } catch (e: Exception) {
        if (e.message == "foo") {
            return "OK"
        }
        return "Exception: ${e.message}"
    }
    return "Exception expected"
}
