// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE
// WITH_STDLIB
fun <T> T.id() = this

const val trimMargin = "123".<!EVALUATED("123")!>trimMargin()<!>

const val trimMarginDefault = """ABC
                |123
                |456""".<!EVALUATED("ABC\n123\n456")!>trimMargin()<!>

const val withoutMargin = """
    #XYZ
    #foo
    #bar
""".<!EVALUATED("XYZ\nfoo\nbar")!>trimMargin("#")<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (trimMargin.id() != "123") return "Fail 1"
    if (trimMarginDefault.id() != "ABC\n123\n456") return "Fail 2"
    if (withoutMargin.id() != "XYZ\nfoo\nbar") return "Fail 3"
    return "OK"
}
