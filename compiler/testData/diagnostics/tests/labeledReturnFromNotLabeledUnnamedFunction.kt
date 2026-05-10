// RUN_PIPELINE_TILL: BACKEND
fun notInline(block: (Boolean) -> Unit): String {
    return ""
}

fun test(): String {
    return notInline(fun(b: Boolean) {
        return@notInline
    })
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, stringLiteral */
