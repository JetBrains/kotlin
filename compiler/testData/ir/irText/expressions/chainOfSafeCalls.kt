// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class C {
    fun foo(): C = this
    fun bar(): C? = this
}

fun test(nc: C?) =
        nc?.foo()?.bar()?.foo()?.foo()
