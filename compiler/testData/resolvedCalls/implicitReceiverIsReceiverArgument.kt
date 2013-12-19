// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: NO_EXPLICIT_RECEIVER
// !THIS_OBJECT: NO_RECEIVER
// !RECEIVER_ARGUMENT: A

class A {}

fun A.foo() {}

fun A.bar() {
    foo()
}
