// !CALL: invoke
// !EXPLICIT_RECEIVER_KIND: BOTH_RECEIVERS
// !THIS_OBJECT: f
// !RECEIVER_ARGUMENT: 1

class Foo() {
    fun Int.invoke() {}
}

fun bar(f: Foo) {
    1.f()
}
