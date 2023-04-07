// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

enum class EnumClass {
    OK, VALUE, anotherValue, WITH_UNDERSCORE
}

const val name1 = EnumClass.OK.<!EVALUATED("OK")!>name<!>
const val name2 = EnumClass.VALUE.<!EVALUATED("VALUE")!>name<!>
const val name3 = EnumClass.anotherValue.<!EVALUATED("anotherValue")!>name<!>
const val name4 = EnumClass.WITH_UNDERSCORE.<!EVALUATED("WITH_UNDERSCORE")!>name<!>

fun box(): String {
    if (<!EVALUATED("false")!>EnumClass.OK.name != "OK"<!>) return "Fail 1"
    if (<!EVALUATED("false")!>name2 != "VALUE"<!>) return "Fail 2"
    if (<!EVALUATED("false")!>name3 != "anotherValue"<!>) return "Fail 3"
    if (<!EVALUATED("false")!>name4 != "WITH_UNDERSCORE"<!>) return "Fail 4"
    return <!EVALUATED("OK")!>name1<!>
}
