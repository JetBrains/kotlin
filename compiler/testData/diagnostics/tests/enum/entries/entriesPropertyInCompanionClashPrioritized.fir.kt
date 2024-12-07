// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class A {
    ;

    companion object {
        val entries = 0
    }
}

fun test() {
    val i: Int = <!INITIALIZER_TYPE_MISMATCH!>A.entries<!>
    A.Companion.entries

    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(A) {
        entries
        this.entries
        <!UNRESOLVED_REFERENCE!>values<!>() // to be sure that we don't resolve into synthetic 'values'
    }

    with(A.Companion) {
        entries
    }

    val aCompanion = A.Companion
    aCompanion.entries
}
