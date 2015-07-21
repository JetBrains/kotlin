fun foo(inlineOptions p: () -> Unit){}

fun bar() {
    foo(<caret>)
}
//Text: (<highlight>p: () -> Unit</highlight>), Disabled: false, Strikeout: false, Green: true