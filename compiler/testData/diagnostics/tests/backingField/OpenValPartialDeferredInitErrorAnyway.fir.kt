// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-ProhibitOpenValDeferredInitialization
open class Foo {
    open <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val foo: Int<!>

    init {
        if (1 != 1) {
            foo = 1
        }
    }
}
