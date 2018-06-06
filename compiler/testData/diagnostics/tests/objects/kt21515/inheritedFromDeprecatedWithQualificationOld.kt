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
    open class c : O.A.FromA()
    open class d : O.B.FromB()

    // VISIBLE: Classifiers from our own companion
    open class n : C.Companion.FromCompanionC()

    // INVISIBLE: direct superclasses themselves.
    open class a : O.A()
    open class b : O.B()

    // DEPRECATED: Classifiers from companions of direct superclasses
    open class e : O.A.Companion.FromCompanionA()
    open class f : O.B.Companion.FromCompanionB()

    // INVISIBLE: "cousin" supertypes themselves
    open class g : O.Alpha()
    open class h : O.Beta()
    open class i : O.Gamma()

    // DEPRECATED: classifiers from "cousin" superclasses
    open class k : O.Alpha.FromAlpha()
    open class l : O.Beta.FromBeta()
    open class m : O.Gamma.FromGamma()

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    open class o : O.Alpha.Companion.FromCompanionAlpha()
    open class p : O.Beta.Companion.FromCompanionBeta()
    open class q : O.Gamma.Companion.FromCompanionGamma()

    // DEPRECATED: Classifiers from supertypes of our own companion
    open class r : Delta.FromDelta()
}