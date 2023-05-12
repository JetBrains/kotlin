// FIR_IDENTICAL
// LANGUAGE:+ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor, +ProhibitOpenValDeferredInitialization
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
open class Foo {
    constructor()

    <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>open val x: String<!>

    init {
        x = ""
    }
}
