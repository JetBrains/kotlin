fun <T> T.foo(): (item: T) -> Unit{}

fun f() {
    val v = "a".foo()
    v(<caret>)
}

/*
Text: (<highlight>item: String</highlight>), Disabled: false, Strikeout: false, Green: true
*/
