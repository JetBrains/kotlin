// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// !DIAGNOSTICS: -UNUSED_VARIABLE

// See KT-21515 for a open class diagram and details

// Object is to prevent accidental short-name import
object O {
    open class Alpha {
        open class FromAlpha

        companion object {
            open class FromCompanionAlpha
        }
    }

    open class Beta : Alpha() {
        open class FromBeta

        companion object {
            open class FromCompanionBeta
        }
    }


    open class A {
        open class FromA

        companion object : Beta() {
            open class FromCompanionA
        }
    }

//////////////////////////

    open class FarAway {
        open class FromFarAway

    }

    open class Gamma {
        open class FromGamma
        companion object : FarAway() {
            open class FromCompanionGamma
        }
    }

    open class B : A() {
        open class FromB

        companion object : Gamma() {
            open class FromCompanionB
        }
    }
}

///////////////////////////////


open class Delta {
    open class FromDelta
}

open class C : O.B() {
    companion object : Delta() {
        open class FromCompanionC
    }

    // VISIBLE: Classifiers from direct superclasses
    open class c : FromA()
    open class d : FromB()

    // VISIBLE: Classifiers from our own companion
    open class n : FromCompanionC()

    // INVISIBLE: direct superclasses themselves.
    open class a : <!UNRESOLVED_REFERENCE!>A<!>()
    open class b : <!UNRESOLVED_REFERENCE!>B<!>()

    // DEPRECATED: Classifiers from companions of direct superclasses
    open class e : <!UNRESOLVED_REFERENCE!>FromCompanionA<!>()
    open class f : <!UNRESOLVED_REFERENCE!>FromCompanionB<!>()

    // INVISIBLE: "cousin" supertypes themselves
    open class g : <!UNRESOLVED_REFERENCE!>Alpha<!>()
    open class h : <!UNRESOLVED_REFERENCE!>Beta<!>()
    open class i : <!UNRESOLVED_REFERENCE!>Gamma<!>()

    // DEPRECATED: classifiers from "cousin" superclasses
    open class k : <!UNRESOLVED_REFERENCE!>FromAlpha<!>()
    open class l : <!UNRESOLVED_REFERENCE!>FromBeta<!>()
    open class m : <!UNRESOLVED_REFERENCE!>FromGamma<!>()

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    open class o : <!UNRESOLVED_REFERENCE!>FromCompanionAlpha<!>()
    open class p : <!UNRESOLVED_REFERENCE!>FromCompanionBeta<!>()
    open class q : <!UNRESOLVED_REFERENCE!>FromCompanionGamma<!>()

    // DEPRECATED: Classifiers from supertypes of our own companion
    open class r : <!UNRESOLVED_REFERENCE!>FromDelta<!>()
}
