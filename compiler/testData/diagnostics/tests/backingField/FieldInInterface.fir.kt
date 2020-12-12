interface My {
    val x: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>
        get() = field
}
