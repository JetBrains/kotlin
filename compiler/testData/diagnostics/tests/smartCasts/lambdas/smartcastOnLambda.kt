// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
fun test(func: (() -> Unit)?) {
    if (func != null) {
        func()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, nullableType, smartcast */
