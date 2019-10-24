/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 5
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 */

enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    return when (x) {
        MyEnum.A -> 1
        is <!IS_ENUM_ENTRY!>MyEnum.<!ENUM_ENTRY_AS_TYPE!>B<!><!> -> 2
        is <!IS_ENUM_ENTRY!>MyEnum.<!ENUM_ENTRY_AS_TYPE!>C<!><!> -> 3
    }
}