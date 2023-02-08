// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

enum class EnumClass {
    OK, VALUE, anotherValue, WITH_UNDERSCORE
}

const val name1 = EnumClass.OK.name
const val name2 = EnumClass.VALUE.name
const val name3 = EnumClass.anotherValue.name
const val name4 = EnumClass.WITH_UNDERSCORE.name

fun box(): String {
    if (EnumClass.OK.name != "OK") return "Fail 1"
    if (name2 != "VALUE") return "Fail 2"
    if (name3 != "anotherValue") return "Fail 3"
    if (name4 != "WITH_UNDERSCORE") return "Fail 4"
    return name1
}