// !DUMP_CFG

abstract class Second {
    val x: String

    init {
        use(this)
        x = bar()
        foo()
    }

    private fun bar() = foo()

    abstract fun foo(): String
}

fun use(second: Second) = second.x

class SecondDerived : Second() {
    val y = x // null!

    override fun foo() = y
}

open class Third {
    open var x: String

    constructor() {
        x = "X" // Non-final property access
    }
}

class ThirdDerived : Third() {
    override var x: String = "Y"
        set(arg) { field = "$arg$y" }

    val y = ""
}

class Fourth {
    val x: String
        get() = y

    val y = x // null!
}
