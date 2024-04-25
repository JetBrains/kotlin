// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// See KT-21515 for a class diagram and details

// class is to prevent accidental short-name import
class O {
    open class Alpha {
        open fun foo() = 42

        class FromAlpha {
            fun foo() = 42
        }

        companion object {
            class FromCompanionAlpha {
                fun foo() = 42
            }
        }
    }

    open class Beta : Alpha() {
        override fun foo() = 42

        class FromBeta {
            fun foo() = 42
        }

        companion object {
            class FromCompanionBeta {
                fun foo() = 42
            }
        }
    }


    open class A {
        open fun foo() = 42

        class FromA {
            fun foo() = 42
        }

        companion object : Beta() {
            class FromCompanionA {
                fun foo() = 42
            }
        }
    }

//////////////////////////

    open class FarAway {
        open fun foo() = 42

        class FromFarAway {
            fun foo() = 42
        }

    }

    open class Gamma {
        open fun foo() = 42

        class FromGamma {
            fun foo() = 42
        }

        companion object : FarAway() {
            class FromCompanionGamma {
                fun foo() = 42
            }
        }
    }

    open class B : A() {
        override fun foo() = 42

        class FromB {
            fun foo() = 42
        }

        companion object : Gamma() {
            override fun foo() = 42

            class FromCompanionB {
                fun foo() = 42
            }
        }
    }
}

///////////////////////////////


open class Delta {
    open fun foo() = 42
    class FromDelta {
        fun foo() = 42
    }
}

class C : O.B() {
    override fun foo() = 42

    companion object : Delta() {
        class FromCompanionC {
            fun foo() = 42
        }
    }

    // VISIBLE: Classifiers from direct superclasses
    val c = FromA::foo
    val d = FromB::foo

    // VISIBLE: Classifiers from our own companion
    val n = FromCompanionC::foo

    // INVISIBLE: direct superclasses themselves.
    val a = <!UNRESOLVED_REFERENCE!>A<!>::foo
    val b = <!UNRESOLVED_REFERENCE!>A<!>::foo

    // DEPRECATED: Classifiers from companions of direct superclasses
    val e = <!UNRESOLVED_REFERENCE!>FromCompanionA<!>::foo
    val f = <!UNRESOLVED_REFERENCE!>FromCompanionB<!>::foo

    // INVISIBLE: "cousin" supertypes themselves
    val g = <!UNRESOLVED_REFERENCE!>Alpha<!>::foo
    val h = <!UNRESOLVED_REFERENCE!>Beta<!>::foo
    val i = <!UNRESOLVED_REFERENCE!>Gamma<!>::foo

    // DEPRECATED: classifiers from "cousin" superclasses
    val k = <!UNRESOLVED_REFERENCE!>FromAlpha<!>::foo
    val l = <!UNRESOLVED_REFERENCE!>FromBeta<!>::foo
    val m = <!UNRESOLVED_REFERENCE!>FromGamma<!>::foo

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    val o = <!UNRESOLVED_REFERENCE!>FromCompanionAlpha<!>::foo
    val p = <!UNRESOLVED_REFERENCE!>FromCompanionBeta<!>::foo
    val q = <!UNRESOLVED_REFERENCE!>FromCompanionGamma<!>::foo

    // DEPRECATED: Classifiers from supertypes of our own companion
    val r = <!UNRESOLVED_REFERENCE!>FromDelta<!>::foo
}
