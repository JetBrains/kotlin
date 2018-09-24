package foo

import foo.Constructors.Base.Companion.FromBaseCompanion
import foo.Constructors.CompanionSupertype.FromCompanionSupertype

object Constructors {

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

class Derived : Constructors.Base() {
    companion object : Constructors.CompanionSupertype() {
    }

    // Constructors
    val e = FromBaseCompanion()
    val f = FromCompanionSupertype()
}