// see https://youtrack.jetbrains.com/issue/KT-21515

object WithFunctionInBase {
    abstract class <!CYCLIC_SCOPES_WITH_COMPANION!>DerivedAbstract<!> : C.Base()

    class Data

    public class C {
        // error-scope
        val data: <!DEPRECATED_ACCESS_BY_SHORT_NAME, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Data<!> = Data()

        open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!>() {
            // error-scope
            fun foo(): <!DEPRECATED_ACCESS_BY_SHORT_NAME, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Int<!> = 42
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
            // error-scope
            val foo: <!DEPRECATED_ACCESS_BY_SHORT_NAME, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Int<!> = 42
        }

        // error-scope
        val data: <!DEPRECATED_ACCESS_BY_SHORT_NAME, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Data<!> = Data()

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
        // Now it is succesfully resolved (vs. ErrorType like in the previous case)
        val data: Data = Data()

        open class <!CYCLIC_SCOPES_WITH_COMPANION!>Base<!>() {
            // Now it is unresolved (vs. ErrorType like in the previous case)
            val foo: <!UNRESOLVED_REFERENCE!>Int<!> = 42

        }

        companion <!CYCLIC_SCOPES_WITH_COMPANION!>object<!> : DerivedAbstract()
    }
}