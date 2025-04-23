// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76365

interface IntA {
    fun check(): String = "OK"
}

interface IntB {
    fun check(): String
}

abstract class AbstractClassA {
    abstract fun check(): String
}

abstract class DerivedA : AbstractClassA(), IntA

object DerivedB : DerivedA(), IntB {
    override fun check(): String {
        return super.<!ABSTRACT_SUPER_CALL!>check<!>()
    }
}
