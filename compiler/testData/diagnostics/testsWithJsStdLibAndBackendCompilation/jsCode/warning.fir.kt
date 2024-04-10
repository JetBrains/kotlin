// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

fun main(): Unit {
    js(<!JSCODE_WARNING!>"var a = 08;"<!>)

    js(<!JSCODE_WARNING!>"""var a =

        08;"""<!>)

    val code = "var a = 08;"
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>code<!>)
}
