// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

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
    val a: <!OTHER_ERROR!>A?<!> = null
    val b: <!OTHER_ERROR!>B?<!> = null

    // DEPRECATED: Classifiers from companions of direct superclasses
    val e: <!OTHER_ERROR!>FromCompanionA?<!> = null
    val f: <!OTHER_ERROR!>FromCompanionB?<!> = null

    // INVISIBLE: "cousin" supertypes themselves
    val g: <!OTHER_ERROR!>Alpha?<!> = null
    val h: <!OTHER_ERROR!>Beta?<!> = null
    val i: <!OTHER_ERROR!>Gamma?<!> = null

    // DEPRECATED: classifiers from "cousin" superclasses
    val k: <!OTHER_ERROR!>FromAlpha?<!> = null
    val l: <!OTHER_ERROR!>FromBeta?<!> = null
    val m: <!OTHER_ERROR!>FromGamma?<!> = null

    // INVISIBLE: We don't see classifiers from companions of "cousin" superclasses
    val o: <!OTHER_ERROR!>FromCompanionAlpha?<!> = null
    val p: <!OTHER_ERROR!>FromCompanionBeta?<!> = null
    val q: <!OTHER_ERROR!>FromCompanionGamma?<!> = null

    // DEPRECATED: Classifiers from supertypes of our own companion
    val r: <!OTHER_ERROR!>FromDelta?<!> = null
}