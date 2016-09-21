// IS_APPLICABLE: false

class Foo {
    <caret>lateinit var bar: String

    fun init() {
        bar = ""
    }
}