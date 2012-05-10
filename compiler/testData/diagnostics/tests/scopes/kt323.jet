//KT-323 Handle visibility interactions with overriding
package kt323

open class A {
    open var a : Int = 0
}

class B : A() {
    override <!VAR_OVERRIDDEN_BY_VAL!>val<!> a = 34

    var b : Int
        <!GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY!>public<!> get() = 23
        set(i: Int) {}

    protected var c : Int
        get() = 23
        private set(i: Int) {}
}
