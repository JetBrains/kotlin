// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// WITH_STDLIB

const val simple = <!EVALUATED("OK 3.5")!>"O${'K'} ${1.toLong() + 2.5}"<!>
const val withInnerConcatenation = <!EVALUATED("1 2 3 4 5 6")!>"1 ${"2 ${3} ${4} 5"} 6"<!>
const val withNull = <!EVALUATED("1 null")!>"1 ${null}"<!> // but `"1" + null` is invalid

fun box(): String {
    if (<!EVALUATED("false")!>simple != "OK 3.5"<!>) return "Fail 1"
    if (<!EVALUATED("false")!>withInnerConcatenation != "1 2 3 4 5 6"<!>) return "Fail 2"
    if (<!EVALUATED("false")!>withNull != "1 null"<!>) return "Fail 3"

    return "OK"
}
