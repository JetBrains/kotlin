// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// ISSUE: KT-39044

fun test(t: Throwable) {
    t.fillInStackTrace()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction */
