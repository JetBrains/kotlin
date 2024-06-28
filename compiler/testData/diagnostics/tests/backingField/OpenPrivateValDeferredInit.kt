// LANGUAGE:+ProhibitOpenValDeferredInitialization
open class Foo {
    <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!><!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>open<!> val foo: Int<!>

    init {
        foo = 1
    }
}
