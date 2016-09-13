val header = { s: String, s1: String -> }
fun header(name: String, value: Int) { }

fun call() {
    header(<caret>"sdf", "sdjfn")
}

/*
Text: (<highlight>name: String</highlight>, value: Int), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>s: String</highlight>, s1: String), Disabled: false, Strikeout: false, Green: true
*/
