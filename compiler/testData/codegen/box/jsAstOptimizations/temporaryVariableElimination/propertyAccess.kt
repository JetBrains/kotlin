// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=b;box expect=propertyAccess.optimized.js TARGET_BACKENDS=JS_IR_ES6

var log = "";

object A {
    val x: Int
        get() {
            log += "A.x;"
            return 23
        }
}

fun b(): Int {
    log += "b();"
    return 42
}

fun box(): String {
    val tmp = A.x
    val result = "${b()};$tmp"

    if (result != "42;23") return "fail1: $result"
    if (log != "A.x;b();") return "fail2: $log"

    return "OK";
}
