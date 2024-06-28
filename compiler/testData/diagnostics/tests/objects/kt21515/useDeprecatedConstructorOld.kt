// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// DIAGNOSTICS: -UNUSED_VARIABLE

// See KT-21515 for a class diagram and details

// Object is to prevent accidental short-name import
object O {
    open class Alpha {
        class FromAlpha

        companion object {
            class FromCompanionAlpha
        }
    }

    open class Beta : Alpha() {
        class FromBeta

        companion object {
            class FromCompanionBeta
        }
    }


    open class A {
        class FromA

        companion object : Beta() {
            class FromCompanionA
        }
    }

//////////////////////////

    open class FarAway {
        class FromFarAway
    }

    open class Gamma {
        class FromGamma
        companion object : FarAway() {
            class FromCompanionGamma
        }
    }

    open class B : A() {
        class FromB

        companion object : Gamma() {
            class FromCompanionB
        }
    }
}

///////////////////////////////


open class Delta {
    class FromDelta
}

class C : O.B() {
    companion object : Delta() {
        class FromCompanionC
    }

    // VISIBLE: Classifiers from direct superclasses
    val c = FromA()
    val d = FromB()

    // VISIBLE: Classifiers from our own companion
    val n = FromCompanionC()

    // INVISIBLE: direct superclasses themselves.
    val a = <!UNRESOLVED_REFERENCE!>A<!>()
    val b = <!UNRESOLVED_REFERENCE!>B<!>()

    // DEPRECATED: Classifiers from companions of direct superclasses
    val e = <!DEPRECATED_ACCESS_BY_SHORT_NAME!>FromCompanionA()<!>
    val f = <!DEPRECATED_ACCESS_BY_SHORT_NAME!>FromCompanionB()<!>

    // INVISIBLE: "cousin" supertypes themselves
    val g = <!UNRESOLVED_REFERENCE!>Alpha<!>()
    val h = <!UNRESOLVED_REFERENCE!>Beta<!>()
    val i = <!UNRESOLVED_REFERENCE!>Gamma<!>()

    // DEPRECATED: classifiers from "cousin" superclasses
    val k = <!DEPRECATED_ACCESS_BY_SHORT_NAME!>FromAlpha()<!>
    val l = <!DEPRECATED_ACCESS_BY_SHORT_NAME!>FromBeta()<!>
    val m = <!DEPRECATED_ACCESS_BY_SHORT_NAME!>FromGamma()<!>

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    val o = <!UNRESOLVED_REFERENCE!>FromCompanionAlpha<!>()
    val p = <!UNRESOLVED_REFERENCE!>FromCompanionBeta<!>()
    val q = <!UNRESOLVED_REFERENCE!>FromCompanionGamma<!>()

    // DEPRECATED: Classifiers from supertypes of our own companion
    val r = <!DEPRECATED_ACCESS_BY_SHORT_NAME!>FromDelta()<!>
}
