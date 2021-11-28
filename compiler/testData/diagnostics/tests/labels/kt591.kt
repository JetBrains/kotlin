//KT-591 Unresolved label in valid code

fun test() {
    val a: (Int?).() -> Unit = a@{
        if (this != null) {
            val b: String.() -> Unit = {
                <!DEBUG_INFO_SMARTCAST!>this@a<!>.times(5) // a@ Unresolved
            }
        }
    }
}
