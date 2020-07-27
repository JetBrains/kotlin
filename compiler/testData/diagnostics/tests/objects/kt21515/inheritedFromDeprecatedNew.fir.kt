// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
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
    open class a : <!OTHER_ERROR!>A<!>()
    open class b : <!OTHER_ERROR!>B<!>()

    // DEPRECATED: Classifiers from companions of direct superclasses
    open class e : <!OTHER_ERROR!>FromCompanionA<!>()
    open class f : <!OTHER_ERROR!>FromCompanionB<!>()

    // INVISIBLE: "cousin" supertypes themselves
    open class g : <!OTHER_ERROR!>Alpha<!>()
    open class h : <!OTHER_ERROR!>Beta<!>()
    open class i : <!OTHER_ERROR!>Gamma<!>()

    // DEPRECATED: classifiers from "cousin" superclasses
    open class k : <!OTHER_ERROR!>FromAlpha<!>()
    open class l : <!OTHER_ERROR!>FromBeta<!>()
    open class m : <!OTHER_ERROR!>FromGamma<!>()

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    open class o : <!OTHER_ERROR!>FromCompanionAlpha<!>()
    open class p : <!OTHER_ERROR!>FromCompanionBeta<!>()
    open class q : <!OTHER_ERROR!>FromCompanionGamma<!>()

    // DEPRECATED: Classifiers from supertypes of our own companion
    open class r : <!OTHER_ERROR!>FromDelta<!>()
}