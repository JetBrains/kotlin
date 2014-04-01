// IS_APPLICABLE: false
trait Foo {
    fun foo(a: Int, b: Int)
}

fun foo(x: Foo) {
    x.<caret>foo(1, 2)
}
