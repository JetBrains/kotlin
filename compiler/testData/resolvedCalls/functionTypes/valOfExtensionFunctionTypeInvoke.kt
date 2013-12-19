// !CALL: invoke
// !EXPLICIT_RECEIVER_KIND: BOTH_RECEIVERS
// !THIS_OBJECT: foo
// !RECEIVER_ARGUMENT: 1

trait A {
    val foo: Int.()->Unit

    fun test() {
        1.foo()
    }
}
