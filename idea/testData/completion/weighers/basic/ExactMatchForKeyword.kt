fun test() {
    val ifMore = true
    val isFirst = true
    if<caret>
}

// ORDER: if
// ORDER: ifMore
// ORDER: isFirst
// ORDER: ifn
// SELECTED: 0