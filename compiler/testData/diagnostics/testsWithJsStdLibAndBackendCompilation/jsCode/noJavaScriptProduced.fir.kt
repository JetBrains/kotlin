// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// ERROR_POLICY: SEMANTIC
fun test() {
    js("")
    js(" ")
    js("""
               """)

    val empty = ""
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>empty<!>)

    val whitespace = "  "
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>whitespace<!>)

    val multiline = """
    """
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>multiline<!>)
}
