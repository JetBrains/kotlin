// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// WITH_STDLIB

const val trimMargin = "123".<!EVALUATED("123")!>trimMargin()<!>

const val trimMarginDefault = """ABC
                |123
                |456""".<!EVALUATED("ABC\n123\n456")!>trimMargin()<!>

const val withoutMargin = """
    #XYZ
    #foo
    #bar
""".<!EVALUATED("XYZ\nfoo\nbar")!>trimMargin("#")<!>

fun box(): String {
    if (<!EVALUATED("false")!>trimMargin != "123"<!>) return "Fail 1"
    if (<!EVALUATED("false")!>trimMarginDefault != "ABC\n123\n456"<!>) return "Fail 2"
    if (<!EVALUATED("false")!>withoutMargin != "XYZ\nfoo\nbar"<!>) return "Fail 3"
    return "OK"
}
