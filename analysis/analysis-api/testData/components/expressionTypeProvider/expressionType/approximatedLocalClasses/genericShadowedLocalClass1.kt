// APPROXIMATE_TYPE

fun test(flag: Boolean) {
    class Foo<T>
    val x = Foo<Int>()

    if (flag) {
        class Fo<caret>o<R>
        <expr>x</expr>
    }
}