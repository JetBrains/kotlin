// FIR_IDENTICAL
// LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

open class Container {
    // Note that here we also have errors and diagnostics, even though there are actually no loops.
    // (this is case because we can't know if there are any loops without resolving, but resolving
    // itself provokes loops)

    interface Base {
        open fun m() {}
    }

    interface DerivedAbstract : Base

    companion object : DerivedAbstract {
        override fun m() {}
    }
}