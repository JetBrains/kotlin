fun returnFun() {}

fun usage() {
    if (true) {
        re<caret>
        return
    }
}

// ORDER: returnFun
// ORDER: return
