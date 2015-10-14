// IS_APPLICABLE: false
// WARNING: 'infix' modifier is inapplicable on this function
interface Foo {
    infix fun foo(a: Int, b: Int)
}

fun foo(x: Foo) {
    x.<caret>foo(1, 2)
}
