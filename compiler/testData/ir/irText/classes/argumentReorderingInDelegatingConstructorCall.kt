// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

open class Base(val x: Int, val y: Int)

class Test1(xx: Int, yy: Int) : Base(y = yy, x = xx)

class Test2 : Base {
    constructor(xx: Int, yy: Int) : super(y = yy, x = xx)
    constructor(xxx: Int, yyy: Int, a: Any) : this(yy = yyy, xx = xxx)
}
