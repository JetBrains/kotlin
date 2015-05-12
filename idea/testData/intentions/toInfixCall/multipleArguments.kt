// IS_APPLICABLE: false
interface Foo {
    fun foo(a: Int, b: Int)
}

fun foo(x: Foo) {
    x.<caret>foo(1, 2)
}
