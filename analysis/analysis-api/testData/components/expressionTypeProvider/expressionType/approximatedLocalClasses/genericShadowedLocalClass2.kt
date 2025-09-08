// APPROXIMATE_TYPE

fun test(flag: Boolean) {
    class Fo<caret>o<T>
    val x = Foo<Int>()

    if (flag) {
        class Foo<R>
        <expr>x</expr>
    }
}