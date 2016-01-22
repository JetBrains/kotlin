inline fun <reified T> foo(p: T) {}

fun bar() {
    foo<<caret>>
}

//Text: (<highlight>reified T</highlight>), Disabled: false, Strikeout: false, Green: false
