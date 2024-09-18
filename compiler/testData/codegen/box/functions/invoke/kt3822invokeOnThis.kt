// IGNORE_BACKEND_K1: JS, JS_ES6
// IGNORE_BACKEND_K2: JS, JS_ES6
//KT-3822 Compiler crashes when use invoke convention with `this` in class which extends Function0<T>
// JS backend does not allow to implement Function{N} interfaces

class B() : Function0<Boolean> {
    override fun invoke() = true

    fun foo() = this() // Exception
}

fun box() = if (B().foo()) "OK" else "fail"