// IGNORE_REVERSED_RESOLVE
open class Base {
    init {
        register(<!DEBUG_INFO_LEAKING_THIS!>this<!>)
        <!DEBUG_INFO_LEAKING_THIS!>foo<!>()
    }

    open fun foo() {}
}

fun register(arg: Base) {
    arg.foo()
}

class Derived(val x: Int) : Base() {
    override fun foo() {
        x.hashCode() // NPE in Base constructor
    }
}

enum class MyEnum {
    FIRST() {
        val x: Int = 42

        override fun foo() {
            x.hashCode() // NPE in MyEnum constructor
        }
    };

    init {
        <!DEBUG_INFO_LEAKING_THIS!>foo<!>()
    }

    abstract fun foo()
}
