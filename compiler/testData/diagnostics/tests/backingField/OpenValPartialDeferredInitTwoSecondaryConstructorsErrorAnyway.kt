// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-ProhibitOpenValDeferredInitialization
open class Foo {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open val foo: Int<!>

    constructor(x: Int) {}
    constructor() {
        foo = 1
    }
}
