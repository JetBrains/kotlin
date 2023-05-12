// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
class Foo {
    <!MUST_BE_INITIALIZED!>var foo: Int<!>
        set(value) {}

    constructor() {
        if (1 != 1) {
            foo = 1
        }
    }
}
