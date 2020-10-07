// "Change type of 'Foo.x' to 'String?'" "true"
class Foo {
    var x = null

    fun foo(condition: Boolean) {
        if (condition) {
            x = "abc"<caret>
        }
    }
}