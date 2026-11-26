// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=se;box expect=transitiveNotConsideredTrivial.optimized.js TARGET_BACKENDS=JS_IR_ES6

var global = 1;

fun se(): Int {
    return global++;
}

fun box(): String {
    val a = se();
    val b = a;
    val result = b + b;
    if (result != 2) return "fail: $result";

    return "OK";
}
