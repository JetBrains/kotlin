// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
class Foo {
    <!MUST_BE_INITIALIZED!>var foo: Int<!>
        set(value) {}

    constructor(x: Int) {}
    constructor() {
        foo = 1
    }
}
