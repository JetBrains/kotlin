val header = { s: String, s1: String -> }
fun header(name: String, value: Int) { }

fun call() {
    header(<caret>"sdf", "sdjfn")
}

//TODO: use parameter names from functional type
/*
Text: (<highlight>String</highlight>, String), Disabled: false, Strikeout: false, Green: true
Text: (<highlight>name: String</highlight>, value: Int), Disabled: false, Strikeout: false, Green: false
*/
