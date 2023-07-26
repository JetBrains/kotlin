// FIR_IDENTICAL
fun regular() {
    <!UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS!>::class<!>

    with(Any()) {
        <!UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS!>::class<!>
    }
}

fun Any.extension() {
    <!UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS!>::class<!>
}

class A {
    fun member() {
        <!UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS!>::class<!>
    }
}