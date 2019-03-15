// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : C.Base() {
    open class Data
}

public class C {

    open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!> ()

    class Foo : <!UNRESOLVED_REFERENCE!>Data<!>()

    companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract()
}