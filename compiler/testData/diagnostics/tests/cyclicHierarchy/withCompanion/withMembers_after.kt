// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

object WithFunctionInBase {
    abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : C.Base()

    class Data

    public class C {
        val data: Data = Data()

        open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!>() {
            fun foo(): Int = 42
        }

        companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract()
    }
}

object WithPropertyInBase {
    // This case is very similar to previous one, but there are subtle differences from POV of implementation

    abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : C.Base()

    class Data

    public class C {

        open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!>() {
            val foo: Int = 42
        }

        val data: Data = Data()

        companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract()
    }
}

object WithPropertyInBaseDifferentOrder {
    // This case is very similar to previous one, but there are subtle differences from POV of implementation
    // Note how position of property in file affected order of resolve, and, consequently, its results and
    // diagnostics.

    abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : C.Base()

    class Data

    public class C {
        val data: Data = Data()

        open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!>() {
            val foo: Int = 42

        }

        companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract()
    }
}