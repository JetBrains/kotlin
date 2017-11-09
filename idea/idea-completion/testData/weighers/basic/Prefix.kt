fun shouldCompleteTopLevelCallablesFromIndex() = true

fun foo(statement: String) {
    if (st<caret>)
}

// ORDER: statement
// ORDER: shouldCompleteTopLevelCallablesFromIndex
