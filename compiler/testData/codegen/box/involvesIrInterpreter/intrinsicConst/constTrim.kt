// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
fun <T> T.id() = this

const val trimMargin1 = "123".<!EVALUATED{IR}("123")!>trimMargin()<!>
const val trimMargin2 = """ABC
                |123
                |456""".<!EVALUATED{IR}("ABC\n123\n456")!>trimMargin()<!>
const val trimMargin3 = """
    #XYZ
    #foo
    #bar
""".<!EVALUATED{IR}("XYZ\nfoo\nbar")!>trimMargin("#")<!>


const val trimIndent1 = "123".<!EVALUATED{IR}("123")!>trimIndent()<!>
const val trimIndent2 =
    """
            ABC
            123
            456
        """.<!EVALUATED{IR}("ABC\n123\n456")!>trimIndent()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (trimMargin1.id() != "123")              return "Fail trimMargin1"
    if (trimMargin2.id() != "ABC\n123\n456")    return "Fail trimMargin2"
    if (trimMargin3.id() != "XYZ\nfoo\nbar")    return "Fail trimMargin3"

    if (trimIndent1.id() != "123")              return "Fail trimIndent1"
    if (trimIndent2.id() != "ABC\n123\n456")    return "Fail trimIndent2"

    return "OK"
}
