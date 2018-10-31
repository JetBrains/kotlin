// !WITH_BASIC_TYPES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: when-expression
 PARAGRAPH: 3
 SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 2
 DESCRIPTION: 'When' without bound value and not allowed comma in when entry.
 */

fun case_1(value_1: _BasicTypesProvider) {
    when {
        getBoolean()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> value_1.getBoolean()  -> return 
        value_1.getBoolean() && getBoolean()<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> getLong() == 1000L -> return 
        <!TYPE_MISMATCH!>value_1.getList()<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> <!TYPE_MISMATCH!>getLong()<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> <!TYPE_MISMATCH!>{}<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> <!TYPE_MISMATCH!>Any()<!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> throw Exception() -> return
    }

    <!UNREACHABLE_CODE!>return<!>
}
