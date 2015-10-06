fun x(name: String, next: (name: String) -> String): String {
    return next(<caret>)
}

//TODO: use parameter names from functional type
/*
Text: (<highlight>p1: String</highlight>), Disabled: false, Strikeout: false, Green: true
*/
