// IS_APPLICABLE: false
// ERROR: 'infix' modifier is inapplicable on this function
interface Foo {
    infix fun foo(a: Int, b: Int)
}

fun foo(x: Foo) {
    x.<caret>foo(1, 2)
}
