// ISSUE: KT-59748
// FIR_DUMP

fun foo(list: MutableList<Any?>, condition: Boolean): Unit = when {
    condition -> list[0] = ""
    else -> Unit
}

fun bar(list: MutableList<Any?>, condition: Boolean): Unit = <!TYPE_MISMATCH!>when {
    condition -> list.set(0, "")
    else -> Unit
}<!>

fun plusFoo(list: MutableList<String>, condition: Boolean): Unit = when {
    condition -> list[0] += ""
    else -> Unit
}

var x: String = ""

fun assign(condition: Boolean): Unit = when {
    condition -> x = ""
    else -> Unit
}

fun plugAssign(condition: Boolean): Unit = when {
    condition -> x += ""
    else -> Unit
}
