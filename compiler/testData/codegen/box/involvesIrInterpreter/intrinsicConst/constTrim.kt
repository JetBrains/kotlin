// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
fun <T> T.id() = this

const val trim1 = "123".<!EVALUATED("123")!>trim()<!>
const val trim2 = """ 123
    456
""".<!EVALUATED("123\n    456")!>trim()<!>


const val trimStart1 = "123".<!EVALUATED("123")!>trimStart()<!>
const val trimStart2 = """ 123 456 """.<!EVALUATED("123 456 ")!>trimStart()<!>

const val trimEnd1 = "123".<!EVALUATED("123")!>trimEnd()<!>
const val trimEnd2 = """ 123 456 """.<!EVALUATED(" 123 456")!>trimEnd()<!>

const val trimMargin1 = "123".<!EVALUATED("123")!>trimMargin()<!>
const val trimMargin2 = """ABC
                |123
                |456""".<!EVALUATED("ABC\n123\n456")!>trimMargin()<!>
const val trimMargin3 = """
    #XYZ
    #foo
    #bar
""".<!EVALUATED("XYZ\nfoo\nbar")!>trimMargin("#")<!>


const val trimIndent1 = "123".<!EVALUATED("123")!>trimIndent()<!>
const val trimIndent2 =
    """
            ABC
            123
            456
        """.<!EVALUATED("ABC\n123\n456")!>trimIndent()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (trim1.id() != "123")                    return "Fail trim1"
    if (trim2.id() != "123\n    456")             return "Fail trim2"

    if (trimStart1.id() != "123")               return "Fail trimStart1"
    if (trimStart2.id() != "123 456 ")          return "Fail trimStart2"

    if (trimEnd1.id() != "123")                 return "Fail trimEnd1"
    if (trimEnd2.id() != " 123 456")            return "Fail trimEnd2"

    if (trimMargin1.id() != "123")              return "Fail trimMargin1"
    if (trimMargin2.id() != "ABC\n123\n456")    return "Fail trimMargin2"
    if (trimMargin3.id() != "XYZ\nfoo\nbar")    return "Fail trimMargin3"

    if (trimIndent1.id() != "123")              return "Fail trimIndent1"
    if (trimIndent2.id() != "ABC\n123\n456")    return "Fail trimIndent2"

    return "OK"
}
