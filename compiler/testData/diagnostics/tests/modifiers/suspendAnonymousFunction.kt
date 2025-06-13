// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57991

fun foo() {
    suspend fun() {

    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration */
