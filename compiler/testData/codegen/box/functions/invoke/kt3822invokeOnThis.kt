//KT-3822 Compiler crashes when use invoke convention with `this` in class which extends Function0<T>

class B() : Function0<Boolean> {
    override fun invoke() = true

    fun foo() = this() // Exception
}

fun box() = if (B().foo()) "OK" else "fail"