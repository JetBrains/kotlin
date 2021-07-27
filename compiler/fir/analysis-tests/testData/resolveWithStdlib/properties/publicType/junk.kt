class Something {
    val junk1 = "some junk"
        get

    val junk2 = "some junk"
        get(): <!REDUNDANT_GETTER_TYPE_CHANGE!>Any?<!>

    val junk3 = "some junk"
        get(): <!REDUNDANT_GETTER_TYPE_CHANGE!>Any?<!> = field

    val junk4 = "some junk"
        get() = field

    protected val junk5 = "some junk"
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>private<!> get

    protected val junk6 = "some junk"
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>private<!> get()

    protected val junk7 = "some junk"
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>private<!> get(): Any? {
            return field
        }
}
