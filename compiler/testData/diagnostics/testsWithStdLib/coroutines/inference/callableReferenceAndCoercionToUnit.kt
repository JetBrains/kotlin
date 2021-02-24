// FIR_IDENTICAL
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -UNUSED_EXPRESSION

fun test(s: String?) {
    val list = buildList {
        s?.let(::add)
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>list<!>
}
