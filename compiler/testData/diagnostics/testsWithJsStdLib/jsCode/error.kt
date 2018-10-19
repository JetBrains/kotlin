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
    js(<!JSCODE_ERROR!>"var = $n;"<!>)

    js(<!JSCODE_ERROR!>code<!>)
}
