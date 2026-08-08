// CHECK_OPTIMIZED_JS

var global = ""

fun foo(x: Int): Int {
    global += x
    return x
}

// EXPECT_GENERATED_JS: function=box expect=labeledContinueInNestedLoop.optimized.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=box expect=labeledContinueInNestedLoop.optimized.es6.js TARGET_BACKENDS=JS_IR_ES6
fun box(): String {
    var i = 0
    loop@ do {
        ++i
        global += ";"
        for (j in 0..<2) {
            if (j == 1 && i == 2) {
                continue@loop
            }
            global += "-"
        }
        if (foo(i) >= 5) {
            break
        }
    } while (true)

    if (global != ";--1;-;--3;--4;--5") return "fail: " + global

    return "OK"
}
