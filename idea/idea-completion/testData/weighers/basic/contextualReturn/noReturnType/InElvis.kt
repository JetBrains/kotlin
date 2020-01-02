fun returnFun() {}

fun usage(a: Int?) {
    a ?: re<caret>
    return
}

// ORDER: return
// ORDER: returnFun
