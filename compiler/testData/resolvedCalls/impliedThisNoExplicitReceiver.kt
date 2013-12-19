// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: NO_EXPLICIT_RECEIVER
// !THIS_OBJECT: Class{A}
// !RECEIVER_ARGUMENT: NO_RECEIVER

class A {
    fun foo() {}
    fun bar() {
        foo()
    }
}