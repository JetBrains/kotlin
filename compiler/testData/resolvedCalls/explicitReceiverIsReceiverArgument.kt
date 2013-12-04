// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: RECEIVER_ARGUMENT
// !THIS_OBJECT: NO_RECEIVER
// !RECEIVER_ARGUMENT: a

class A {}

fun A.foo() {}

fun bar(a: A) {
    a.foo()
}
