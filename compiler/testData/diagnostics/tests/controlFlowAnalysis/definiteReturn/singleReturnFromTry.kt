// FIR_IDENTICAL
// DUMP_CFG
// DIAGNOSTICS: -UNUSED_EXPRESSION
// ISSUE: KT-56476

inline fun <R> myRun(block: () -> R): R {
    return block()
}

fun test_1(): Int {
    myRun {
        return 2
    }
}

fun test_2(): Int {
    try {
        return 2
    } finally {}
}

fun test_3(): Int {
    try {
        myRun {
            return 2
        }
    } finally {}
}

fun test_4(): Int {
    myRun {
        try {
            return 2
        } finally {}
    }
}

fun test_5(): Int { // should be an error about missing return
    try {
        myRun {
            return 2
        }
    } catch (e: Exception) {
        "hello"
    } finally {}
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
