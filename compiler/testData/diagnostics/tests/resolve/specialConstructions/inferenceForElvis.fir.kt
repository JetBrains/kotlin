// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !CHECK_TYPE

fun <T: Any> foo(f: (T) -> Unit): T? = null // T is used only as return type
fun test() {
    val x = foo { it checkType { _<String>() }} ?: "" // foo() is inferred as foo<String>, which isn't very good
    val y: Any = foo { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>it<!> checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Any>() } } ?: "" // but for now it's fixed by specifying expected type
}