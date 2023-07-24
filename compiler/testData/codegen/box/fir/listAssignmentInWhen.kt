// ISSUE: KT-59748
// WITH_STDLIB

fun foo(list: MutableList<Any?>, condition: Boolean): Unit = when {
    condition -> list[0] = "OK"
    else -> Unit
}

fun box(): String {
    val list = mutableListOf<Any?>("FAIL")
    foo(list, true)
    foo(list, false)
    return list[0] as String
}
