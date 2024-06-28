// SKIP_TXT

fun case_1(vararg args: dynamic) {
    (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Nothing..kotlin.Any?!>")!>listOf(null) + args<!>).toTypedArray()
}

fun case_2(vararg args: dynamic) {
    (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Nothing..kotlin.Any?!>")!>listOf<Nothing>() + args<!>).toTypedArray()
}

fun case_3(vararg args: dynamic) {
    (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Nothing..kotlin.Any?!>")!>listOf(1) + args<!>).toTypedArray()
}

fun case_4(vararg args: dynamic) {
    (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Nothing..kotlin.Any?!>")!>listOf<Any?>() + args<!>).toTypedArray()
}

fun case_5(x: Any?) {
    fun foo(x: List<dynamic>) = x

    foo(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Nothing..kotlin.Any?!>")!>listOf(null) + x as MutableList<out dynamic><!>)
}

fun case_6(x: Any?) {
    (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Any?>")!>listOf(null) + x as MutableList<in dynamic><!>).toTypedArray()
}
