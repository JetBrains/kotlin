// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

open class Container {
    open class Base {
        open fun m() {}
    }

    // note that Base() supertype will be resolved in scope that was created on recursion
    abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : <!UNRESOLVED_REFERENCE!>Base<!>()

    companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract() {
        <!NOTHING_TO_OVERRIDE!>override<!> fun m() {}
    }
}
