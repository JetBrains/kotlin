// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

data class A(val x: Int, val y: Int)

var fn: (A) -> Int = { (_, y) -> 42 + y }
