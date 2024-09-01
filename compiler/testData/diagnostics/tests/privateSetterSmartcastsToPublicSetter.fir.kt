// ISSUE: KT-68521
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

open class Base {
    protected open var publicGetInDerived: Int
        get() = 10
        set(value) {}

    protected open var publicInDerived: Int
        get() = 15
        set(value) {}

    open var publicInDerived2: Int
        get() = 17
        protected set(value) {}
}

class Derived : Base() {
    override public var publicGetInDerived: Int = 20
        protected set(value) { field = value }

    override public var publicInDerived: Int = 25

    override public var publicInDerived2: Int = 27
}

fun main(test: Base) {
    if (test !is Derived) return

    test.<!INVISIBLE_SETTER!>publicGetInDerived<!> = 5
    test.<!INVISIBLE_SETTER!>publicGetInDerived<!> -= 5
    test.<!INVISIBLE_SETTER!>publicGetInDerived<!>--
    --test.<!INVISIBLE_SETTER!>publicGetInDerived<!>

    test.publicInDerived = 5
    test.publicInDerived -= 5
    test.publicInDerived--
    --test.publicInDerived

    test.publicInDerived2 = 5
    test.publicInDerived2 -= 5
    test.publicInDerived2--
    --test.publicInDerived2
}
