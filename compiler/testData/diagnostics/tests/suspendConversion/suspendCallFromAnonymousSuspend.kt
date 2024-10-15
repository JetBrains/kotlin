// RUN_PIPELINE_TILL: SOURCE
// FIR_DUMP

fun foo() {
    suspend fun() {
        bar()
    }
}

suspend fun bar() {

}
