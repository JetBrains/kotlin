// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JS_IR

fun testEmptyString() {
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>""<!>)
}

const val SPACE = " "

fun testStringWithSpaces() {
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>"\n \n$SPACE \t" + " "<!>)
}

fun testComment() {
    js(<!JSCODE_NO_JAVASCRIPT_PRODUCED!>"""
        // just a comment
        $SPACE
    """<!>)
}
