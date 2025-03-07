// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809

interface I {
    fun x(a: Int): String
}

class A: I {
    override fun x(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE, UNDERSCORE_IS_RESERVED!>_<!>: Int) = ""
}

