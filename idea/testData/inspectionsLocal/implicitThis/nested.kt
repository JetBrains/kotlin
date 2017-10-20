// WITH_RUNTIME

class Foo {
    fun s() = ""

    fun test() {
        "".apply {
            <caret>s()
        }
    }
}