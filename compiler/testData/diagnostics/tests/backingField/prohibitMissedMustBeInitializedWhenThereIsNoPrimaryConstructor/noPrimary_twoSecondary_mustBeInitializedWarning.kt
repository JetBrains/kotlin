// FIR_IDENTICAL
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
class Foo {
    constructor()
    constructor(x: Int)
    <!MUST_BE_INITIALIZED_WARNING!>var x: String<!>
        set(value) {}

    init {
        x = ""
    }
}
