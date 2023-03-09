// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

interface ILeft {
    fun foo() {}
    val bar: Int get() = 1
}

interface IRight {
    fun foo() {}
    val bar: Int get() = 2
}

class CBoth : ILeft, IRight {
    override fun foo() {
        super<ILeft>.foo()
        super<IRight>.foo()
    }

    override val bar: Int
        get() = super<ILeft>.bar + super<IRight>.bar
}
