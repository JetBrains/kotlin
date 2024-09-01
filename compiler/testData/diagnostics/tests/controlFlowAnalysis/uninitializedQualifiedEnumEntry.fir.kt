// LANGUAGE: +ProhibitQualifiedAccessToUninitializedEnumEntry
// ISSUE: KT-41124

enum class SomeEnum11(var x: Int) {
    A(1),
    B(2);

    init {
        <!UNINITIALIZED_ENUM_ENTRY!>A<!>.x = 10
    }
}

enum class SomeEnum12(var x: Int) {
    A(1),
    B(2);

    init {
        SomeEnum12.<!UNINITIALIZED_ENUM_ENTRY!>A<!>.x = 10
    }
}

enum class SomeEnum21(var x: Int) {
    A(1) {
        init {
            <!UNINITIALIZED_ENUM_ENTRY!>A<!>.x = 10
            SomeEnum21.<!UNINITIALIZED_ENUM_ENTRY!>A<!>.x = 10
            <!UNINITIALIZED_ENUM_ENTRY!>B<!>.x = 10
        }
    },
    B(2)
}

enum class SomeEnum22(var x: Int) {
    A(1) {
        init {
            <!UNINITIALIZED_ENUM_ENTRY!>A<!>.x = 10
            SomeEnum22.<!UNINITIALIZED_ENUM_ENTRY!>A<!>.x = 10
            SomeEnum22.<!UNINITIALIZED_ENUM_ENTRY!>B<!>.x = 10
        }
    },
    B(2)
}


enum class SomeEnum3(var x: Int) {
    A(1),
    B(2) {
        init {
            A.x = 10
            SomeEnum3.A.x = 10
            <!UNINITIALIZED_ENUM_ENTRY!>B<!>.x = 10
            SomeEnum3.<!UNINITIALIZED_ENUM_ENTRY!>B<!>.x = 10
        }
    };
}
