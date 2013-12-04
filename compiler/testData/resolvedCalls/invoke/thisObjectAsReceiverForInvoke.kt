// !CALL: invoke
// !EXPLICIT_RECEIVER_KIND: THIS_OBJECT
// !THIS_OBJECT: f
// !RECEIVER_ARGUMENT: NO_RECEIVER

class Foo {
    fun invoke() {}
}

fun bar(f: Foo) {
    f()
}
