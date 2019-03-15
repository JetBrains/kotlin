fun test(list: List<String>) {
    foo(list.toTypedArray().spread<caret>)
}

fun foo(vararg args: String) {}