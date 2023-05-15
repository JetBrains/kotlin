// FIR_IDENTICAL
// LANGUAGE:+ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
open class Foo {
    constructor()

    <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>open var x: String<!>

    init {
        x = ""
    }
}
