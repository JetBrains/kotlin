fun <T> run(block: () -> T) : T = block()

fun test1() {
    run {
        if (true) {
            <!INVALID_IF_AS_EXPRESSION, IMPLICIT_CAST_TO_ANY!>if (true) {}<!>
        }
        else {
            <!IMPLICIT_CAST_TO_ANY!>1<!>
        }
    }
}
