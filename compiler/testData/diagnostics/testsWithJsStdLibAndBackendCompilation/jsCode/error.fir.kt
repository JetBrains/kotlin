// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// ERROR_POLICY: SEMANTIC
val code = """
    var s = "hello"
    + );
"""

fun main(): Unit {
    js(<!JSCODE_ERROR!>"var = 10;"<!>)

    js(<!JSCODE_ERROR!>"""var = 10;"""<!>)

    js(<!JSCODE_ERROR!>"""var
      = 777;
    """<!>)

    js(<!JSCODE_ERROR!>"""
    var = 777;
    """<!>)

    js(<!JSCODE_ERROR!>"var " + " = " + "10;"<!>)

    val n = 10
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var = $<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>n<!>;"<!>)

    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>code<!>)
}
