// DIAGNOSTICS: -USELESS_ELVIS -UNUSED_EXPRESSION

class X {
    fun toLong(): Long? = TODO()
}

fun getLong(): Long = TODO()

fun test_1(list: List<X>) {
    val props = list.map { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long?")!>it.toLong()<!> ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>0<!> }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Long>")!>props<!>
}

fun test_2(cond: Boolean) {
    val props = if (cond) getLong() else 0
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>props<!>
}

fun test_3(list: List<X>) {
    val props = list.map { Pair(it.toLong() ?: 0, it.toLong() ?: 0) }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Pair<kotlin.Long, kotlin.Long>>")!>props<!>
}