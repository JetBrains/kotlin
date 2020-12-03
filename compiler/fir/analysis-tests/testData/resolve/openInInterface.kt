interface Some {
    open fun foo()
    open fun bar() {}

    open val x: Int
    open val y = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
    open val z get() = 1

    open var xx: Int
    open var yy = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
    open var zz: Int
        set(value) {
            field = value
        }
}