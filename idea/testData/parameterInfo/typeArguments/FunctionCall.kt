fun <T, V> foo(t: T): T = t

fun bar() {
    foo<<caret>>()
}

//Text: (<highlight>T</highlight>, V), Disabled: false, Strikeout: false, Green: false
