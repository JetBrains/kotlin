// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: THIS_OBJECT
// !THIS_OBJECT: a
// !RECEIVER_ARGUMENT: NO_RECEIVER

trait A {
    val foo: (Int)->Int
}

fun test(a: A) {
    a.foo(1)
}