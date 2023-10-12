// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// ERROR_POLICY: SEMANTIC
fun main(): Unit {
    js("var a =<!JSCODE_WARNING!> 08<!>;")

    js("""var a =<!JSCODE_WARNING!>

        08<!>;""")

    val code = "var a = 08;"
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_WARNING!>code<!>)
}
