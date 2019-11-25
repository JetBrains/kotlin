// IGNORE_BACKEND_FIR: JVM_IR
var subjectEvaluated = 0

fun String.foo() = length.also { ++subjectEvaluated }

fun test(s: String?) =
    when (s?.foo()) {
        0 -> "zero"
        1 -> "one"
        2 -> "two"
        else -> "other"
    }

fun box(): String {
    val t = test("12")
    if (t != "two") return "Fail: $t"
    if (subjectEvaluated != 1) return "Fail: subjectEvaluated=$subjectEvaluated"

    return "OK"
}