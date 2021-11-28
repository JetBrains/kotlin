// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: TypesProvider) {
    when {
        getBoolean()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> value_1.getBoolean()  -> return
        value_1.getBoolean() && getBoolean()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> getLong() == 1000L -> return
        <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>Out<Int>()<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> getLong()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> {}<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> Any()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> throw Exception() -> return
    }

    return
}
