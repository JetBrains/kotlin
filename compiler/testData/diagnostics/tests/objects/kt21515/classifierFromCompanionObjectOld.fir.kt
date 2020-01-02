// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
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
    val c: FromA? = null
    val d: FromB? = null

    // VISIBLE: Classifiers from our own companion
    val n: FromCompanionC? = null

    // INVISIBLE: direct superclasses themselves.
    val a: A? = null
    val b: B? = null

    // DEPRECATED: Classifiers from companions of direct superclasses
    val e: FromCompanionA? = null
    val f: FromCompanionB? = null

    // INVISIBLE: "cousin" supertypes themselves
    val g: Alpha? = null
    val h: Beta? = null
    val i: Gamma? = null

    // DEPRECATED: classifiers from "cousin" superclasses
    val k: FromAlpha? = null
    val l: FromBeta? = null
    val m: FromGamma? = null

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    val o: FromCompanionAlpha? = null
    val p: FromCompanionBeta? = null
    val q: FromCompanionGamma? = null

    // DEPRECATED: Classifiers from supertypes of our own companion
    val r: FromDelta? = null
}