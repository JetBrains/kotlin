// WITH_RUNTIME

fun test1() {
    Foo().apply {
        <caret>this.s = ""
    }
}