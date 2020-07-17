class C {
    private companion object
}

typealias CAlias = C

val <!EXPOSED_PROPERTY_TYPE!>test1<!> = CAlias
val <!EXPOSED_PROPERTY_TYPE!>test1a<!> = C