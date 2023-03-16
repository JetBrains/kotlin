// !LANGUAGE: +IntrinsicConstEvaluation

enum class EnumClass {
    OK, VALUE, anotherValue, WITH_UNDERSCORE
}

const val name1 = EnumClass.OK.name
const val name2 = EnumClass.VALUE.name
const val name3 = EnumClass.anotherValue.name
const val name4 = EnumClass.WITH_UNDERSCORE.name
