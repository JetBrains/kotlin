// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=id;box expect=firstBranchIsConsidered.optimized.js TARGET_BACKENDS=JS_IR_ES6

/*
this is similar to shortCircuit, but `c || d` is placed before `a || b`, allowing c and d to be eliminated, since
that expression (`c || d`) is always evaluated.
*/

var log = ""

fun id(x: Int): Int {
    log += "$x;"
    return x
}

fun box(): String {
    val a = id(2)
    val b = id(3)
    val c = id(4)
    val d = id(5)

    if (c > d || a > b) return "fail condition"
    if (log != "2;3;4;5;") return "fail log: $log"

    return "OK"
}
