interface Some {
    <!REDUNDANT_OPEN_IN_INTERFACE{LT}!><!REDUNDANT_OPEN_IN_INTERFACE{PSI}!>open<!> fun foo()<!>
    open fun bar() {}

    <!REDUNDANT_OPEN_IN_INTERFACE{LT}!><!REDUNDANT_OPEN_IN_INTERFACE{PSI}!>open<!> val x: Int<!>
    open val y = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
    open val z get() = 1

    <!REDUNDANT_OPEN_IN_INTERFACE{LT}!><!REDUNDANT_OPEN_IN_INTERFACE{PSI}!>open<!> var xx: Int<!>
    open var yy = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
    open var zz: Int
        set(value) {
            field = value
        }
}
