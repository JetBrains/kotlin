// RUN_PIPELINE_TILL: CODEGEN
inline fun <T1, T2, reified T3, T4> foo(): T3 = null!!

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, inline, nullableType, reified, typeParameter */
