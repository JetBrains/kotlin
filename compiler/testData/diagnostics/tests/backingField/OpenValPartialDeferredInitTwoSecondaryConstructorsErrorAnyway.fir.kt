// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-ProhibitOpenValDeferredInitialization
open class Foo {
    open <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val foo: Int<!>

    constructor(x: Int) {}
    constructor() {
        foo = 1
    }
}
