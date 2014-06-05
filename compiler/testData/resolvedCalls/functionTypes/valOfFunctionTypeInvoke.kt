trait A {
    val foo: (Int)->Int
}

fun test(a: A) {
    a.foo<caret>(1)
}