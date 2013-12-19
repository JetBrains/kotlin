// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: THIS_OBJECT
// !THIS_OBJECT: a
// !RECEIVER_ARGUMENT: NO_RECEIVER

class A {
    fun foo() {}
}

fun bar(a: A) {
    a.foo()
}
