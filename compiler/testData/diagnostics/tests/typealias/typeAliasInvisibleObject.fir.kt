class C {
    private companion object
}

typealias CAlias = C

val <!EXPOSED_PROPERTY_TYPE!>test1<!> = <!INVISIBLE_REFERENCE!>CAlias<!>
val <!EXPOSED_PROPERTY_TYPE!>test1a<!> = <!INVISIBLE_REFERENCE!>C<!>
