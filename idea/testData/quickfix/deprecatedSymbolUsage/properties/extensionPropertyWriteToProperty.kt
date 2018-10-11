// "Replace with 'm'" "true"

private class C {
    var m: String = ""
}

@Deprecated("", ReplaceWith("m"))
private var C.old: String
    get() = m
    set(value) {
        m = value
    }

private fun use(c: C, s: String) {
    c.old<caret> = s
}