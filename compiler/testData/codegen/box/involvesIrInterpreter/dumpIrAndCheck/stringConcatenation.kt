// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// WITH_STDLIB

const val simple = "O${'K'} ${1.toLong() + 2.5}"
const val withInnerConcatenation = "1 ${"2 ${3} ${4} 5"} 6"
const val withNull = "1 ${null}" // but `"1" + null` is invalid

fun box(): String {
    if (simple != "OK 3.5") return "Fail 1"
    if (withInnerConcatenation != "1 2 3 4 5 6") return "Fail 2"
    if (withNull != "1 null") return "Fail 3"

    return "OK"
}
