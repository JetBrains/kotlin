// !CALL: invoke

trait A {
    val foo: (Int)->Int
}

fun test(a: A) {
    a.foo(1)
}