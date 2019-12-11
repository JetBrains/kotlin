fun inlineCall(): String {
    inlineFun {
        if (true) {
            return@inlineCall ""
        }
        1
    }

    return "x"
}

inline fun inlineFun(s: ()->Int) {
    s()
}


fun noInlineCall(): String {
    noInline {
        if (true) {
            return@noInlineCall ""
        }
        1
    }

    return "x"
}


fun noInline(s: ()->Int) {
    s()
}