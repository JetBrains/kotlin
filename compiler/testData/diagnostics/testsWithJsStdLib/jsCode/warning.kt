fun main(args: Array<String>): Unit {
    js("var a =<!JSCODE_WARNING!> 08<!>;")

    js("""var a =<!JSCODE_WARNING!>

        08<!>;""")

    val code = "var a = 08;"
    js(<!JSCODE_WARNING!>code<!>)
}