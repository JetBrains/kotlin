// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

open class Props {
    val a: Int = 0
    val b: Int = 0
}

class ScopeHost {
    fun fromType(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Props<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {
        <!UNRESOLVED_REFERENCE!>a<!>
        <!UNRESOLVED_REFERENCE!>b<!>
    }
}

class Host : Props() {
    fun source(a: Int, b: String) {}

    fun fromFunction(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>source<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

    fun target(a: Int, b: Int) {}

    fun useBound() {
        target(...Props.$props(this))
        target(...Props.$props(this).exclude(a), a = 1)
    }
}

fun topTarget(a: Int, b: Int) {}

fun topLevel() {
    topTarget(...Props.$props(<!NO_THIS!>this<!>))
}
