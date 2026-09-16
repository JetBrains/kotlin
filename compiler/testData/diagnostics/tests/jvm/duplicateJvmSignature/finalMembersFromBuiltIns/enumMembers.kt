// RUN_PIPELINE_TILL: CODEGEN
// IGNORE_FIR_DIAGNOSTICS
// IGNORE_ERRORS

enum class E {
    ENTRY;

    fun <!VIRTUAL_MEMBER_HIDDEN!>getDeclaringClass<!>() {}
    fun <!VIRTUAL_MEMBER_HIDDEN!>finalize<!>() {}
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration */
