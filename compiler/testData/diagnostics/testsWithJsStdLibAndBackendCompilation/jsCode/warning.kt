// RUN_PIPELINE_TILL: CODEGEN
// IGNORE_FIR_DIAGNOSTICS

fun main(): Unit {
    js(<!JSCODE_WARNING!>"var a = 08;"<!>)

    js(<!JSCODE_WARNING!>"""var a =

        08;"""<!>)

    val code = "var a = 08;"
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>code<!>)
}
