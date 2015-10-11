val header = { s: String, s1: String -> }
fun header(name: String, value: Int) { }

fun call() {
    header(<caret>"sdf", "sdjfn")
}

//TODO: use parameter names from functional type
/*
Text: (<highlight>name: String</highlight>, value: Int), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>p1: String</highlight>, p2: String), Disabled: false, Strikeout: false, Green: true
*/
