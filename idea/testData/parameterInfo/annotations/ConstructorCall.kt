annotation class Fancy

class Foo(@Fancy foo: Int)

fun bar() {
    Foo(<caret>)
}

/*
Text: (<highlight>@Fancy foo: Int</highlight>), Disabled: false, Strikeout: false, Green: true
*/
