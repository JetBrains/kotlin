// LANGUAGE:+ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
open class Foo {
    constructor()

    open <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>var x: String<!>

    init {
        x = ""
    }
}
