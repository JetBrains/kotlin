// IS_APPLICABLE: false
// ERROR: 'infix' modifier is inapplicable on this function

fun foo(x: Foo) {
    x.<caret>foo(1) { it * 2 }
}

interface Foo {
    infix fun foo(a: Int, f: (Int) -> Int)
}
