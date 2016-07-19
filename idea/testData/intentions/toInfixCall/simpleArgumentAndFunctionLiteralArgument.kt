// IS_APPLICABLE: false
// ERROR: 'infix' modifier is inapplicable on this function: must have a single value parameter

fun foo(x: Foo) {
    x.<caret>foo(1) { it * 2 }
}

interface Foo {
    infix fun foo(a: Int, f: (Int) -> Int)
}
