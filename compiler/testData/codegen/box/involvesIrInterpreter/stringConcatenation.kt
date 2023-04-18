// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// WITH_STDLIB
fun <T> T.id() = this

const val simple = <!EVALUATED("OK 3.5")!>"O${'K'} ${1.toLong() + 2.5}"<!>
const val withInnerConcatenation = <!EVALUATED("1 2 3 4 5 6")!>"1 ${"2 ${3} ${4} 5"} 6"<!>
const val withNull = <!EVALUATED("1 null")!>"1 ${null}"<!> // but `"1" + null` is invalid

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (simple.id() != "OK 3.5") return "Fail 1"
    if (withInnerConcatenation.id() != "1 2 3 4 5 6") return "Fail 2"
    if (withNull.id() != "1 null") return "Fail 3"

    return "OK"
}
