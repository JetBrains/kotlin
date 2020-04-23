// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR

open class Base {
    open fun setup() {}
    init { setup() }
}

class Derived : Base {
    constructor() : super()
    override fun setup() { x = 1 }

    // Technically, this field initializer comes after the superclass
    // constructor is called. However, we optimize away field initializers
    // which set fields to their default value, which is why x ends up with
    // value 1 after the constructor call.
    var x = 0
}

fun box(): String = if (Derived().x == 1) "OK" else "Fail"
