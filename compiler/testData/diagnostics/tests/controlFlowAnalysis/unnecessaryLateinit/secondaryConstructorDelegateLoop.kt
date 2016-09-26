// !DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    constructor() {
        bar = ""
    }

    constructor(a: Int) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(a, 0, 0) {
    }

    constructor(a: Int, b: Int) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(a) {
    }

    constructor(a: Int, b: Int, c: Int) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(a, b) {
    }
}
