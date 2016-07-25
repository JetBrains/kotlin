// INTENTION_TEXT: Remove unnecessary lateinit

class Foo {
    <caret>lateinit var bar: String

    init {
        bar = ""
    }
}