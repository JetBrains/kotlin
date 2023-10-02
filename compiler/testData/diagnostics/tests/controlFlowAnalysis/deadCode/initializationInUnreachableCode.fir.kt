// ISSUE: KT-40851

fun error(): Nothing = throw Exception()

class Some<!UNREACHABLE_CODE!>()<!> {
    var x: Int
    val y: Int = error()

    init {
        <!UNREACHABLE_CODE!>x = 1<!>;
    }
}
