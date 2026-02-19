// APPROXIMATE_TYPE

fun test(flag: Boolean) {
    class F<caret>oo
    val x = Foo()

    if (flag) {
        class Foo
        <expr>x</expr>
    }
}