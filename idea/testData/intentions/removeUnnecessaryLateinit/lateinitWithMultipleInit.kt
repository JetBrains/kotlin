// INTENTION_TEXT: Remove unnecessary lateinit

class Foo {
    <caret>lateinit var bar: String
    var baz: Int

    init {
        baz = 1
    }

    init {
        bar = ""
    }
}