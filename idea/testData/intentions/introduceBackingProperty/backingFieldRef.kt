class Foo {
    var <caret>x = ""
        get() = $x + "!"
        set(value) { $x = value + "!" }

    fun foo(): String {
        return $x
    }
}
