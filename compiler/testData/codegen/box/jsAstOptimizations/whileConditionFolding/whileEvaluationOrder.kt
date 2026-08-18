// CHECK_OPTIMIZED_JS

var global = ""

fun foo(x: Int): Int {
    global += "$x;"
    return x
}

// EXPECT_GENERATED_JS: function=box expect=whileEvaluationOrder.optimized.js
fun box(): String {
    var i = 1
    var sum = 0
    while (true) {
        if (foo(i) >= 10) {
            break
        }
        if (foo(sum) > 30) {
            break
        }
        sum += i
        i++
    }

    if (global != "1;0;2;1;3;3;4;6;5;10;6;15;7;21;8;28;9;36;") return "fail1: " + global
    if (sum != 36) return "fail2: " + sum

    return "OK"
}
