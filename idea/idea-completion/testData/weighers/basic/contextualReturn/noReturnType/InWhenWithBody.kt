fun returnFun() {}

fun usage(a: Int) {
    when (a) {
        10 -> {
            re<caret>
        }
    }
    return
}

// ORDER: return
// ORDER: returnFun
