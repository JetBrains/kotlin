// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// IGNORE_FIR_DIAGNOSTICS

enum class E {
    ENTRY;

    fun <!VIRTUAL_MEMBER_HIDDEN!>getDeclaringClass<!>() {}
    fun <!VIRTUAL_MEMBER_HIDDEN!>finalize<!>() {}
}
