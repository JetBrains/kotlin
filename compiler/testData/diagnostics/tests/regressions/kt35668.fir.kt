// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun main() {
    val baseDir: String? = ""
    val networkParameters: String? = ""
    if (baseDir != null) {
        if (networkParameters != null) {
            Unit
        } else if (true){
            return
        } else {
            return
        }
    } else {
        return
    }

    networkParameters.length // unsafe call
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral */
