// LANGUAGE: -ProhibitOpenValDeferredInitialization
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
open class A {
    open <!MUST_BE_INITIALIZED_WARNING!>val c: Int<!>
        <!VAL_WITH_SETTER!>set(value) {}<!>

    init {
        c = 1
    }
}
