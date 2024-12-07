// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// IGNORE_BACKEND_K1: JS_IR
fun test() {
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>""<!>)
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>" "<!>)
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>"""
               """<!>)

    val empty = ""
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_NO_JAVASCRIPT_PRODUCED!>empty<!>)

    val whitespace = "  "
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_NO_JAVASCRIPT_PRODUCED!>whitespace<!>)

    val multiline = """
    """
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_NO_JAVASCRIPT_PRODUCED!>multiline<!>)
}
