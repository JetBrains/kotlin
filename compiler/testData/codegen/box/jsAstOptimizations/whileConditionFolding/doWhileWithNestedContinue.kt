// CHECK_OPTIMIZED_JS

var global = ""

fun foo(x: Int): Int {
    global += x
    return x
}

// EXPECT_GENERATED_JS: function=box expect=doWhileWithNestedContinue.optimized.js
fun box(): String {
    var i = 0
    var j: Int
    do {
        ++i
        global += ";"
        for (k in arrayOf("a", "b")) {
            if (k != "a") {
                continue
            }
            global += "@"
        }
        j = 0
        while (j++ < 2) {
            if (j == 1) {
                continue
            }
            global += "$"
        }
        j = 0
        do {
            if (j == 1) {
                continue
            }
            global += "#"
        } while (j++ < 2)
        if (foo(i) >= 3) {
            break
        }
    } while (true)

    if (global != ";@$##1;@$##2;@$##3") return "fail: " + global

    return "OK"
}
