// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
//KT-3822 Compiler crashes when use invoke convention with `this` in class which extends Function0<T>
// IGNORE_BACKEND: JS
// JS backend does not allow to implement Function{N} interfaces

class B() : Function0<Boolean> {
    override fun invoke() = true

    fun foo() = this() // Exception
}

fun box() = if (B().foo()) "OK" else "fail"