// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

open class Container {
    // Note that here we also have errors and diagnostics, even though there are actually no loops.
    // (this is case because we can't know if there are any loops without resolving, but resolving
    // itself provokes loops)

    interface Base {
        open fun m() {}
    }

    interface <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : <!UNRESOLVED_REFERENCE!>Base<!>

    companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract {
        <!NOTHING_TO_OVERRIDE!>override<!> fun m() {}
    }
}