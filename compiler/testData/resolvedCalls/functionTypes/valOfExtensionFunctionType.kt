// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: NO_EXPLICIT_RECEIVER
// !THIS_OBJECT: Class{A}
// !RECEIVER_ARGUMENT: NO_RECEIVER

trait A {
    val foo: Int.()->Unit

    fun test() {
        1.foo()
    }
}
