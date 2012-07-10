// http://youtrack.jetbrains.net/issue/KT-418

fun ff() {
    val i: Int = 1
    val <!UNUSED_VARIABLE!>a<!>: Int = i<!UNNECESSARY_SAFE_CALL!>?.<!>plus(2)
}
