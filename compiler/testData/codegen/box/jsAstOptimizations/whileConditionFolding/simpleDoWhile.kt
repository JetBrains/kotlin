// CHECK_OPTIMIZED_JS

var global = ""

fun foo(x: Int): Int {
    global += x
    return x
}

// EXPECT_GENERATED_JS: function=box expect=simpleDoWhile.optimized.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=box expect=simpleDoWhile.optimized.es6.js TARGET_BACKENDS=JS_IR_ES6
fun box(): String {
    var i = 0
    do {
        ++i
        global += ";"
        if (foo(i) >= 10) {
            break
        }
    } while (true)

    if (global != ";1;2;3;4;5;6;7;8;9;10") return "fail: " + global

    return "OK"
}
