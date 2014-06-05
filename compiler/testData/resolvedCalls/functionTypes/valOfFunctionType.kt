trait A {
    val foo: (Int)->Int
}

fun test(a: A) {
    a.<caret>foo(1)
}