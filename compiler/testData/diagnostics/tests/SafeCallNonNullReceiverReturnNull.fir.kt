fun Int.gg() = null

fun ff() {
    val a: Int = 1
    val b: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>a<!UNNECESSARY_SAFE_CALL!>?.<!>gg()<!>
}
