// FIR_COMPARISON
fun test() {
    val ifMore = true
    val isFirst = true
    if<caret>
}

// ORDER: if
// ORDER: ifMore
// ORDER: isFirst
// SELECTED: 0