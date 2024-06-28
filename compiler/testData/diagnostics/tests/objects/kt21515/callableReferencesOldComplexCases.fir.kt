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
        val a = <!UNRESOLVED_REFERENCE!>FromBaseCompanion<!>::foo
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
        val a = <!UNRESOLVED_REFERENCE!>FromBaseCompanion<!>::foo
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
        val a = <!UNRESOLVED_REFERENCE!>FromBaseCompanion<!>::foo
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
        val a = <!UNRESOLVED_REFERENCE!>FromBaseCompanion<!>::foo
    }
}
