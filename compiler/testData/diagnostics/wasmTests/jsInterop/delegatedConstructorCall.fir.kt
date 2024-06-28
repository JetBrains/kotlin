external open class Base(x: Int) {
    constructor(x: String) : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>this<!>(23)

    constructor(x: String, y: String) : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>this<!>("")
}

external open class Derived1() : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>Base(23)<!> {
    constructor(x: Byte) : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>super<!>(23)

    constructor(x: String) : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>super<!>("")

    constructor(x: String, y: String) : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>super<!>("")
}

external open class Derived2() : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>Base("")<!>
