// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

object WithFunctionInBase {
    abstract class DerivedAbstract : C.Base()

    class Data

    public class C {
        // error-scope
        val data: Data = Data()

        open class Base() {
            // error-scope
            fun foo(): Int = 42
        }

        companion object : DerivedAbstract()
    }
}

object WithPropertyInBase {
    // This case is very similar to previous one, but there are subtle differences from POV of implementation

    abstract class DerivedAbstract : C.Base()

    class Data

    public class C {

        open class Base() {
            // error-scope
            val foo: Int = 42
        }

        // error-scope
        val data: Data = Data()

        companion object : DerivedAbstract()
    }
}

object WithPropertyInBaseDifferentOrder {
    // This case is very similar to previous one, but there are subtle differences from POV of implementation
    // Note how position of property in file affected order of resolve, and, consequently, its results and
    // diagnostics.

    abstract class DerivedAbstract : C.Base()

    class Data

    public class C {
        // Now it is successfully resolved (vs. ErrorType like in the previous case)
        val data: Data = Data()

        open class Base() {
            // Now it is unresolved (vs. ErrorType like in the previous case)
            val foo: Int = 42

        }

        companion object : DerivedAbstract()
    }
}