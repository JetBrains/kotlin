package foo

import foo.CallableReferenceOnClass.Base.Companion.FromBaseCompanion
import foo.CallableReferenceOnClass.CompanionSupertype.FromCompanionSupertype

object CallableReferenceOnClass {

    open class Base {
        companion object {
            class FromBaseCompanion {
                fun foo() = 42
            }
        }
    }

    open class CompanionSupertype {
        class FromCompanionSupertype {
            fun foo() = 42
        }
    }
}

class Derived : CallableReferenceOnClass.Base() {
    companion object : CallableReferenceOnClass.CompanionSupertype() { }

    // Callable references
    val c = FromBaseCompanion::foo
    val d = FromCompanionSupertype::foo
}