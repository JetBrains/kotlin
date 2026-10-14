// RUN_PIPELINE_TILL: CODEGEN
suspend fun acceptSuspend(f: (suspend () -> String)?) {
}

suspend fun <T : (() -> String)?> test(f : T) {
    acceptSuspend(<!DEBUG_INFO_EXPRESSION_TYPE("(suspend () -> kotlin.String)?"), DEBUG_INFO_EXPRESSION_TYPE("T")!>f<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionTypeConversion, functionalType, nullableType, suspend,
typeConstraint, typeParameter */
