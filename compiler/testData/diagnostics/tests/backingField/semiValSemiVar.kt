// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-ProhibitOpenValDeferredInitialization
open class Foo {
    <!MUST_BE_INITIALIZED_WARNING!>open val x: String<!>
        <!VAL_WITH_SETTER!>set(value) {}<!>

    init {
        x = ""
    }
}
