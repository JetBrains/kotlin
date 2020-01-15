// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// !DIAGNOSTICS: -UNUSED_VARIABLE

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
    val c = O.A.FromA()
    val d = O.B.FromB()

    // VISIBLE: Classifiers from our own companion
    val n = C.Companion.FromCompanionC()

    // INVISIBLE: direct superclasses themselves.
    val a = O.A()
    val b = O.B()

    // DEPRECATED: Classifiers from companions of direct superclasses
    val e = O.A.<!AMBIGUITY!>Companion<!>.<!UNRESOLVED_REFERENCE!>FromCompanionA<!>()
    val f = O.B.<!AMBIGUITY!>Companion<!>.<!UNRESOLVED_REFERENCE!>FromCompanionB<!>()

    // INVISIBLE: "cousin" supertypes themselves
    val g = O.Alpha()
    val h = O.Beta()
    val i = O.Gamma()

    // DEPRECATED: classifiers from "cousin" superclasses
    val k = O.Alpha.FromAlpha()
    val l = O.Beta.FromBeta()
    val m = O.Gamma.FromGamma()

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    val o = O.Alpha.Companion.FromCompanionAlpha()
    val p = O.Beta.Companion.FromCompanionBeta()
    val q = O.Gamma.Companion.FromCompanionGamma()

    // DEPRECATED: Classifiers from supertypes of our own companion
    val r = Delta.FromDelta()
}