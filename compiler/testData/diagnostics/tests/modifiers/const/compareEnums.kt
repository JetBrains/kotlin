// FIR_IDENTICAL
// DIAGNOSTICS: -TYPE_MISMATCH -CONDITION_TYPE_MISMATCH

enum class EnumClass {
    VALUE1, VALUE2
}

const val equal1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 == EnumClass.VALUE2<!>
const val equal2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 != EnumClass.VALUE2<!>
const val compareTo1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 > EnumClass.VALUE2<!>
const val compareTo2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 <= EnumClass.VALUE2<!>
const val equality1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 === EnumClass.VALUE2<!>
const val equality2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 !== EnumClass.VALUE2<!>
const val and = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 && EnumClass.VALUE2<!>
const val or = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE1 || EnumClass.VALUE2<!>

annotation class Anno(val i: Boolean)
@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>EnumClass.VALUE1 == EnumClass.VALUE2<!>)
class MyClass
