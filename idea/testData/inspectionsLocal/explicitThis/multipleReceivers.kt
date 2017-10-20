// WITH_RUNTIME

class Foo {
    val s = ""

    fun test() {
        "".apply {
            <caret>this@Foo.s
        }
    }
}