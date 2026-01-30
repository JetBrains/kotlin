// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-51538
// WITH_STDLIB

suspend fun other() {}

suspend fun outer(a: List<Int>, acc: List<Int>): List<Int> {
    fun inner(): List<Int> {
        return a.flatMap {
            <!ILLEGAL_SUSPEND_FUNCTION_CALL!>other<!>()
            <!ILLEGAL_SUSPEND_FUNCTION_CALL!>outer<!>(a, acc + listOf(it))
        }
    }
    return inner()
}
