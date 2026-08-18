// CHECK_OPTIMIZED_JS

var global = ""

fun foo(x: Int): Int {
    global += x
    return x
}

// EXPECT_GENERATED_JS: function=box expect=doWhileWithContinue.optimized.js
fun box(): String {
    var i = 0
    do {
        ++i
        if (i == 5) {
            continue
        }
        global += ";"
        if (foo(i) >= 10) {
            break
        }
    } while (true)

    if (global != ";1;2;3;4;6;7;8;9;10") return "fail: " + global

    return "OK"
}
