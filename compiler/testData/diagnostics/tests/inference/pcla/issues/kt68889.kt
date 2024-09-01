// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68889

fun main(s: String?) {
    val a = buildList {
        val (_, matchResult) = s?.let { 1 to it } ?: return@buildList
        add(matchResult)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>a<!>
}
