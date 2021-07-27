class A {
    protected val number = 4
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>internal<!> get(): Number

    internal val items = mutableListOf("-", "+")
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>protected<!> get(): <!WRONG_GETTER_RETURN_TYPE!>Number<!>
}
