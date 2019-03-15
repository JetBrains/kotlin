
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 3 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: 'When' without bound value and not allowed comma in when entry.
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
