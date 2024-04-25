// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

// ===== Case 1: LHS is a class
//
object A {
    open class Base {
        companion object {
            class FromBaseCompanion {
                fun foo() = 42
            }
        }
    }

    class Derived : Base() {
        val a = A.Base.Companion.FromBaseCompanion::foo
    }
}

// ===== Case 2: LHS is a class with companion object, function comes from class

object B {
    open class Base {
        companion object {
            class FromBaseCompanion {
                fun foo() = 42

                companion object {}
            }
        }
    }

    class Derived : Base() {
        val a = B.Base.Companion.FromBaseCompanion::foo
    }
}

// ==== Case 3: LHS is a class with companion object, function comes from companion

object C {
    open class Base {
        companion object {
            class FromBaseCompanion {
                companion object {
                    fun foo() = 42
                }
            }
        }
    }

    class Derived : Base() {
        val a = <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>C.Base.Companion.FromBaseCompanion::foo<!>
    }
}

// ==== Case 4: LHS is an object

object D {
    open class Base {
        companion object {
            object FromBaseCompanion {
                fun foo() = 42
            }
        }
    }

    class Derived : Base() {
        val a = D.Base.Companion.FromBaseCompanion::foo
    }
}
