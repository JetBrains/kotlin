// FIR_IDENTICAL
// !LANGUAGE: -IntrinsicConstEvaluation

enum class EnumClass {
    OK, VALUE, anotherValue, WITH_UNDERSCORE
}

const val name1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.OK.name<!>
const val name2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE.name<!>
const val name3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.anotherValue.name<!>
const val name4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.WITH_UNDERSCORE.name<!>
