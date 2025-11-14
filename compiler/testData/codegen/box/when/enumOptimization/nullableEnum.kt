// CHECK_CASES_COUNT: function=mytest count=3
// CHECK_IF_COUNT: function=mytest count=0

enum class E {
    A,
    B
}

fun mytest(e: E?) = when (e) {
    E.A -> "Fail A"
    null -> "OK"
    E.B -> "Fail B"
}

fun box(): String {
    return mytest(null)
}

// CHECK_BYTECODE_TEXT
// 1 TABLESWITCH
