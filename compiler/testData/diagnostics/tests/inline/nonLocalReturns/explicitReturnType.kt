fun inlineCallExplicitError(): String {
    inlineFun @lamba {
        if (true) {
            <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@lamba 2<!>
        }
        1
    }

    return "x"
}

fun inlineCall(): String {
    inlineFun @lamba {
        (): Int ->
        if (true) {
            return@lamba 2
        }
        1
    }

    return "x"
}

inline fun inlineFun(s: () -> Int) {
    s()
}


fun noInlineCall(): String {
    noInline @lambda {
        (): Int ->
        if (true) {
            return@lambda 2
        }
        1
    }
    return "x"
}


fun noInline(s: () -> Int) {
    s()
}