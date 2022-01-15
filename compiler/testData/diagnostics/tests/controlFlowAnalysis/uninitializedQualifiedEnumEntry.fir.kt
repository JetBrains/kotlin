// LANGUAGE: +ProhibitQualifiedAccessToUninitializedEnumEntry
// ISSUE: KT-41124

enum class SomeEnum11(var x: Int) {
    A(1),
    B(2);

    init {
        A.x = 10 // Error
    }
}

enum class SomeEnum12(var x: Int) {
    A(1),
    B(2);

    init {
        SomeEnum12.A.x = 10 // Error
    }
}

enum class SomeEnum21(var x: Int) {
    A(1) {
        init {
            A.x = 10 // OK
            SomeEnum21.A.x = 10 // OK
            B.x = 10 // Error
        }
    },
    B(2)
}

enum class SomeEnum22(var x: Int) {
    A(1) {
        init {
            A.x = 10 // OK
            SomeEnum22.A.x = 10 // OK
            SomeEnum22.B.x = 10 // Migration error
        }
    },
    B(2)
}


enum class SomeEnum3(var x: Int) {
    A(1),
    B(2) {
        init {
            A.x = 10 // OK
            SomeEnum3.A.x = 10 // OK
            B.x = 10 // OK
            SomeEnum3.B.x = 10 // OK
        }
    };
}
