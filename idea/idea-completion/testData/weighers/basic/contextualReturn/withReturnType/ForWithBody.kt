fun returnFun(): Int = 10

fun usage(): Int {
    for (i in 1..10) {
        re<caret>
    }

    return 10
}

// ORDER: returnFun
// ORDER: return
