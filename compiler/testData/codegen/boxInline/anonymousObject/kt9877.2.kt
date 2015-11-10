package test

fun <T> T.noInline(p: (T) -> Unit) {
    p(this)
}

inline fun inlineCall(p: () -> Unit) {
    p()
}

