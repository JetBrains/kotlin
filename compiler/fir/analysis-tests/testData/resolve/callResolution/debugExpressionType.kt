fun foo(
    x1: String,
    x2: Collection<CharSequence>,
    x3: MutableMap<out CharSequence, in MutableList<*>>
) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Collection<kotlin.CharSequence>")!>x2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<out kotlin.CharSequence, in kotlin.collections.MutableList<*>>")!>x3<!>
}
