// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

open class Base(val x: Any)

object Host {
    class Derived1 : Base(this)
    class Derived2 : Base(Host)
}
