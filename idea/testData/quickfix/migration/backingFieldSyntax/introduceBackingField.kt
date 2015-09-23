// "Introduce backing property" "true"

class Foo {
    var x = ""
        get() = $x + "!"
        set(value) { $x = value + "!" }

    fun foo(): String {
        return $<caret>x
    }
}
