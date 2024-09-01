// LANGUAGE:+ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor, -ProhibitOpenValDeferredInitialization
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
open class Foo {
    constructor()

    open <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING!>val x: String<!>

    init {
        x = ""
    }
}
