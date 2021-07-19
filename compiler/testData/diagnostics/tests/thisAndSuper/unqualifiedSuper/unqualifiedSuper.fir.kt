//  Base  Interface
//     \  /
//      \/
//    Derived
//

open class Base() {
    open fun foo() {}

    open fun ambiguous() {}

    open val prop: Int
        get() = 1234

    open val ambiguousProp: Int
        get() = 111
}

interface Interface {
    fun bar() {}

    fun ambiguous() {}

    val ambiguousProp: Int
        get() = 222
}

class Derived : Base(), Interface {
    override fun foo() {}
    override fun bar() {}

    override fun ambiguous() {}

    override val ambiguousProp: Int
        get() = 333

    override val prop: Int
        get() = 4321

    fun callsFunFromSuperClass() {
        super.foo()
    }

    fun getSuperProp(): Int =
            super.prop

    fun getAmbiguousSuperProp(): Int =
            <!AMBIGUOUS_SUPER!>super<!>.ambiguousProp

    fun callsFunFromSuperInterface() {
        super.bar()
    }

    fun callsAmbiguousSuperFun() {
        <!AMBIGUOUS_SUPER!>super<!>.ambiguous()
    }
}
