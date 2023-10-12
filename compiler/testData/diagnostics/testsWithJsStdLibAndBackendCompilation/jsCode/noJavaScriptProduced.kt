fun test() {
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>""<!>)
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>" "<!>)
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>"""
               """<!>)

    val empty = ""
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>empty<!>)

    val whitespace = "  "
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>whitespace<!>)

    val multiline = """
    """
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>multiline<!>)
}