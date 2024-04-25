// FIR_IDENTICAL
// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
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
    val c: O.A.FromA? = null
    val d: O.B.FromB? = null

    // VISIBLE: Classifiers from our own companion
    val n: C.Companion.FromCompanionC? = null

    // INVISIBLE: direct superclasses themselves.
    val a: O.A? = null
    val b: O.B? = null

    // DEPRECATED: Classifiers from companions of direct superclasses
    val e: O.A.Companion.FromCompanionA? = null
    val f: O.B.Companion.FromCompanionB? = null

    // INVISIBLE: "cousin" supertypes themselves
    val g: O.Alpha? = null
    val h: O.Beta? = null
    val i: O.Gamma? = null

    // DEPRECATED: classifiers from "cousin" superclasses
    val k: O.Alpha.FromAlpha? = null
    val l: O.Beta.FromBeta? = null
    val m: O.Gamma.FromGamma? = null

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    val o: O.Alpha.Companion.FromCompanionAlpha? = null
    val p: O.Beta.Companion.FromCompanionBeta? = null
    val q: O.Gamma.Companion.FromCompanionGamma? = null

    // DEPRECATED: Classifiers from supertypes of our own companion
    val r: Delta.FromDelta? = null
}