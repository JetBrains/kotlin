// !CALL: invoke
// !EXPLICIT_RECEIVER_KIND: RECEIVER_ARGUMENT
// !THIS_OBJECT: NO_RECEIVER
// !RECEIVER_ARGUMENT: f

class Foo
fun Foo.invoke() {}

fun bar(f: Foo) {
    f()
}
