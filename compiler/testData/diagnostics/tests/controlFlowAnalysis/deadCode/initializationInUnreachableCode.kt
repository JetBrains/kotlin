// ISSUE: KT-40851

fun error(): Nothing = throw Exception()

class Some() {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var x: Int<!>
    val y: Int = error()

    init {
        x = 1;
    }
}
