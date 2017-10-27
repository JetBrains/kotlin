annotation class Fancy

private fun abc(@Fancy foo: Int) {
}

fun foo() {
    abc(<caret>)
}

/*
Text: (<highlight>@Fancy foo: Int</highlight>), Disabled: false, Strikeout: false, Green: true
*/
