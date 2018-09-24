// see https://youtrack.jetbrains.com/issue/KT-21515

abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : C.Base()

class Data

open class C {
    open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!> {
        open fun m() {}
    }

    val field = Data()

    companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract() {
        override fun m() {}
    }
}