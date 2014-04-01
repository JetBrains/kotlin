// IS_APPLICABLE: false
fun foo(x: Foo) {
    x.<caret>foo(1) { it * 2 }
}

trait Foo {
    fun foo(a: Int, f: (Int) -> Int)
}
