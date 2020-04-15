// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 2 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: When without bound value, forbidden comma in the when condition.
 * HELPERS: typesProvider, classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: TypesProvider) {
    when {
        getBoolean(), value_1.getBoolean()  -> return
        value_1.getBoolean() && getBoolean(), getLong() == 1000L -> return
        Out<Int>(), getLong(), {}, Any(), throw Exception() -> return
    }

    return
}
