package foo

import foo.CallableReferenceOnClassWithCompanion.Base.Companion.FromBaseCompanion
import foo.CallableReferenceOnClassWithCompanion.CompanionSupertype.FromCompanionSupertype

object CallableReferenceOnClassWithCompanion {

    open class Base {
        companion object {
            class FromBaseCompanion {
                fun foo() = 42

                // We need it to cover another code-path
                companion object
            }
        }
    }

    open class CompanionSupertype {
        class FromCompanionSupertype {
            fun foo() = 42

            // We need it to cover another code-path
            companion object
        }
    }
}

class Derived : CallableReferenceOnClassWithCompanion.Base() {
    companion object : CallableReferenceOnClassWithCompanion.CompanionSupertype() { }

    // Callable references
    val c = FromBaseCompanion::foo
    val d = FromCompanionSupertype::foo
}