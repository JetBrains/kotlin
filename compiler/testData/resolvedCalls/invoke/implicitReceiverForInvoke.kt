// !CALL: invoke
// !EXPLICIT_RECEIVER_KIND: THIS_OBJECT
// !THIS_OBJECT: f
// !RECEIVER_ARGUMENT: Int

class Foo() {
    fun Int.invoke() {}
}

fun bar(f: Foo, i: Int) {
    with (i) {
        f()
    }
}

fun <T, R> with(receiver: T, f: T.() -> R) : R = throw Exception()