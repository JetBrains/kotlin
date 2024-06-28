// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

import A.Base.Companion.FromABaseCompanion
import B.Base.Companion.FromBBaseCompanion
import C.Base.Companion.FromCBaseCompanion
import D.Base.Companion.FromDBaseCompanion

// ===== Case 1: LHS is a class
//
object A {
    open class Base {
        companion object {
            class FromABaseCompanion {
                fun foo() = 42
            }
        }
    }

    class Derived : Base() {
        val a = FromABaseCompanion::foo
    }
}

// ===== Case 2: LHS is a class with companion object, function comes from class

object B {
    open class Base {
        companion object {
            class FromBBaseCompanion {
                fun foo() = 42

                companion object {}
            }
        }
    }

    class Derived : Base() {
        val a = FromBBaseCompanion::foo
    }
}

// ==== Case 3: LHS is a class with companion object, function comes from companion

object C {
    open class Base {
        companion object {
            class FromCBaseCompanion {
                companion object {
                    fun foo() = 42
                }
            }
        }
    }

    class Derived : Base() {
        val a = <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>FromCBaseCompanion::foo<!>
    }
}

// ==== Case 4: LHS is an object

object D {
    open class Base {
        companion object {
            object FromDBaseCompanion {
                fun foo() = 42
            }
        }
    }

    class Derived : Base() {
        val a = FromDBaseCompanion::foo
    }
}
