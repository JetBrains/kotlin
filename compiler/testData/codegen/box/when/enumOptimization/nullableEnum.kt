// CHECK_CASES_COUNT: function=test count=3
// CHECK_IF_COUNT: function=test count=0

enum class E {
    A,
    B
}

fun test(e: E?) = when (e) {
    E.A -> "Fail A"
    null -> "OK"
    E.B -> "Fail B"
}

fun box(): String {
    return test(null)
}

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 TABLESWITCH
