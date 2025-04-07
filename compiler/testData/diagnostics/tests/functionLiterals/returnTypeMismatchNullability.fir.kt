// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTICS_MESSAGES
fun test(a: List<Int?>) {
    val b: List<Int> = a.map { <!RETURN_TYPE_MISMATCH("Int; Int")!>it?.let { c -> c }<!> }
}
