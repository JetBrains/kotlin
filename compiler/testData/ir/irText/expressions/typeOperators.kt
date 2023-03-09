// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

interface IThing

fun test1(x: Any) = x is IThing
fun test2(x: Any) = x !is IThing
fun test3(x: Any) = x as IThing
fun test4(x: Any) = x as? IThing
