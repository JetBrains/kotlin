import <!UNRESOLVED_IMPORT!>incorrect<!>.directory.My

open class My : <!CYCLIC_INHERITANCE_HIERARCHY!>My<!>()

open class Your : <!CYCLIC_INHERITANCE_HIERARCHY!>His<!>()

open class His : <!CYCLIC_INHERITANCE_HIERARCHY!>Your<!>()
