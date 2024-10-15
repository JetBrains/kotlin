// RUN_PIPELINE_TILL: SOURCE
// LANGUAGE:+ProhibitOpenValDeferredInitialization
open class Foo {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>open<!> val foo: Int

    init {
        foo = 1
    }
}
