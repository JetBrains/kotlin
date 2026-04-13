// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58460

fun someFunction() : Any {
    <!RETURN_TYPE_MISMATCH!>return<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration */
