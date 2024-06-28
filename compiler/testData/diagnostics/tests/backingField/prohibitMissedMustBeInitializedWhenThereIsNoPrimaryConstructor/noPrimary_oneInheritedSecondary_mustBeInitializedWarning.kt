// FIR_IDENTICAL
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
open class Base(x: Int)
class Foo : Base {
    constructor() : super(1)
    <!MUST_BE_INITIALIZED_WARNING!>var x: String<!>
        set(value) {}

    init {
        x = ""
    }
}
