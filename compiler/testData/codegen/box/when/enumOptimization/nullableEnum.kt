// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 1.9.20 2.0.0 2.1.0 2.2.0
// ^^^ function `test` is codegenerated to `test_0`, and test directive `// CHECK_CASES_COUNT: function=test` does not see it

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
// 1 TABLESWITCH
