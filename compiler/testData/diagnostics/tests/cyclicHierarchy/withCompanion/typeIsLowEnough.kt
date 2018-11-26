// see https://youtrack.jetbrains.com/issue/KT-21515

abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : C.Base() {
    override abstract fun m()
}

public class C {
    class Data

    open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!> () {
        open fun m() {}
    }

    // Note that Data is resolved successfully here because we don't step on error-scope
    val data: Data = Data()

    companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract() {
        override fun m() {}
    }
}