package a

fun foo() {
    val i : Int? = 42
    if (i != null) {
        <!UNRESOLVED_REFERENCE!>doSmth<!> {
            val x = <!DEBUG_INFO_SMARTCAST!>i<!> + 1
        }
    }
}
