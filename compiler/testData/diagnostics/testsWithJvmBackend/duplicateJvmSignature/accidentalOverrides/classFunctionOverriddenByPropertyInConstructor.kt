// RUN_PIPELINE_TILL: KLIB
// FIR_IDENTICAL
open class B {
    fun getX() = 1
}

class C(<!ACCIDENTAL_OVERRIDE!>val x: Int<!>) : B()