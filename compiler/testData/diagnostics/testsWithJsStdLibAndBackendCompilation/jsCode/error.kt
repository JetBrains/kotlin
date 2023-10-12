// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// ERROR_POLICY: SEMANTIC
val code = """
    var s = "hello"
    + );
"""

fun main(): Unit {
    js("var<!JSCODE_ERROR!> =<!> 10;")

    js("""var<!JSCODE_ERROR!> =<!> 10;""")

    js("""var<!JSCODE_ERROR!>
      =<!> 777;
    """)

    js("""
    var<!JSCODE_ERROR!> =<!> 777;
    """)

    js(<!JSCODE_ERROR!>"var " + " = " + "10;"<!>)

    val n = 10
    js(<!JSCODE_ERROR!>"var = $<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>n<!>;"<!>)

    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION, JSCODE_ERROR!>code<!>)
}
