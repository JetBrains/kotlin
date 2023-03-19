// ISSUE: KT-43846

fun test_1(x: Any): String {
    if (x is String) {
        val thunk = { x }
        return thunk()
    }
    return "str"
}

fun test_2(x: Any): String {
    if (x is String) {
        val thunk = { x + "a" }
        return thunk()
    }
    return "str"
}
