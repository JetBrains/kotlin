// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-68667

fun foo(list: List<List<String>?>) {
    val some = list.mapNotNull { it ?: arrayListOf() }
}
