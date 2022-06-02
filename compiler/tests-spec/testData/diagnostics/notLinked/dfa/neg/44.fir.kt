// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-25747
 */
fun case_1(x: Int?) {
    val y = x != null
    if (y) {
        x.inv()
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-25747
 */
fun case_2(x: Any?) {
    val y = x is Int
    if (y) {
        x.inv()
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-25747
 */
fun <T> case_3(x: T) {
    val y = x is Int
    if (y) {
        x.inv()
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-25747
 */
fun <T> case_4(x: T) {
    val y = x is Int?
    if (y) {
        x?.inv()
    }
}
