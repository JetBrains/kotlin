// FIR_IDENTICAL
// FIR_DUMP

fun foo() {
    suspend fun() {
        bar()
    }
}

suspend fun bar() {

}
