// APPROXIMATE_TYPE

fun test(flag: Boolean) {
    class Foo
    val x = Foo()

    if (flag) {
        class Fo<caret>o
        <expr>x</expr>
    }
}