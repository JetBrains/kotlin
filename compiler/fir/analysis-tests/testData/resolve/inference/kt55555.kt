// ISSUE: KT-55555

private interface Foo

private class Bar<T : Foo, L : List<T>>

private fun <T : Foo> bar(t: T): Bar<T, *> = null!!

private fun <T : Foo> foo(t: T): T {
    val map =
        if (t is Bar<*, *>) t <!UNCHECKED_CAST!>as Bar<T, *><!>
        else bar(t)
    return t
}
