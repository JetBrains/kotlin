// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

enum class <!CONFLICTING_JVM_DECLARATIONS!>E<!> {
    A;

    <!CONFLICTING_JVM_DECLARATIONS!>fun values(): Array<E><!> = null!!
    <!CONFLICTING_JVM_DECLARATIONS!>fun valueOf(s: String): E<!> = null!!
}

/* GENERATED_FIR_TAGS: checkNotNullCall, enumDeclaration, enumEntry, functionDeclaration */
