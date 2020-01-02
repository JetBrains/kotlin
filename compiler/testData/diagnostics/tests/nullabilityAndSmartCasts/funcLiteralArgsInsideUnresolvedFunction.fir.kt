package a

fun foo() {
    val i : Int? = 42
    if (i != null) {
        <!UNRESOLVED_REFERENCE!>doSmth<!> {
            val x = i + 1
        }
    }
}