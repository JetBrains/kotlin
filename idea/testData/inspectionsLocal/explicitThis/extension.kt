class Foo {
    fun test() {
        <caret>this.s()
    }
}

fun Foo.s() = ""