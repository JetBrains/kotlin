// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
class Foo {
    <!MUST_BE_INITIALIZED!>var x: String<!>
        set(value) {}

    init {
        x = ""
    }
}
