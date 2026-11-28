// RUN_PIPELINE_TILL: CODEGEN
// RENDER_DIAGNOSTICS_FULL_TEXT
interface User {
    fun medicalRecord(): String | AccessDenied
}

error class AccessDenied

fun <T : User | AccessDenied> foo(user: User | AccessDenied, t : T) {
    user|.<!MERGED_ERRORS_IN_SAFE_CALL!>medicalRecord()<!>
    t|.<!MERGED_ERRORS_IN_SAFE_CALL!>medicalRecord()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, safeCall */
