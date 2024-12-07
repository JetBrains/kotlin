// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

fun foo() {
    suspend fun() {
        bar()
    }
}

suspend fun bar() {

}
