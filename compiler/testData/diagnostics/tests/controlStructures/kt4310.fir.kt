package f

fun test(a: Boolean, b: Boolean): Int {
    return if(a) {
        1
    } else {
        <!INVALID_IF_AS_EXPRESSION!>if<!> (b) {
            3
        }
    }
}
