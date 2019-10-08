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
        getBoolean()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> value_1.getBoolean()  -> return 
        value_1.getBoolean() && getBoolean()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> getLong() == 1000L -> return 
        <!TYPE_MISMATCH!>Out<Int>()<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> <!TYPE_MISMATCH!>getLong()<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> <!TYPE_MISMATCH!>{}<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> <!TYPE_MISMATCH!>Any()<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> throw Exception() -> return
    }

    <!UNREACHABLE_CODE!>return<!>
}
