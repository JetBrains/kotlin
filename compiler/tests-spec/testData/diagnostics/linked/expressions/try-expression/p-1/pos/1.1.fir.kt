// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

fun throwException(): Nothing = throw Exception()


// TESTCASE NUMBER: 1

fun case1() {
    try {
        throwException()
    }finally {
        "a"
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    try {
        val a = throwException()
    }catch (e: Exception) {
        "a"
    }
}
