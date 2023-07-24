// ISSUE: KT-59748
// FIR_DUMP

fun foo(list: MutableList<Any?>, condition: Boolean): Unit = <!RETURN_TYPE_MISMATCH!>when {
    condition -> list[0] = ""
    else -> Unit
}<!>

fun bar(list: MutableList<Any?>, condition: Boolean): Unit = <!RETURN_TYPE_MISMATCH!>when {
    condition -> list.set(0, "")
    else -> Unit
}<!>
