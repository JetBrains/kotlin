// ISSUE: KT-45970

val a_1: Byte = 1
var a_2: Byte = 1
val b_1: Short = 1
var b_2: Short = 1
val c_1: Int = 1
var c_2: Int = 1
val d_1: Long = 1
var d_2: Long = 1

val e_1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
var e_2: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
val f_1: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
var f_2: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
val g_1: Int = 1 + 2
var g_2: Int = 1 + 2
val h_1: Long = 1 + 2
var h_2: Long = 1 + 2

fun local() {
    val a_1: Byte = 1
    var a_2: Byte = 1
    val b_1: Short = 1
    var b_2: Short = 1
    val c_1: Int = 1
    var c_2: Int = 1
    val d_1: Long = 1
    var d_2: Long = 1

    val e_1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    var e_2: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    val f_1: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    var f_2: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    val g_1: Int = 1 + 2
    var g_2: Int = 1 + 2
    val h_1: Long = 1 + 2
    var h_2: Long = 1 + 2
}

class Member {
    val a_1: Byte = 1
    var a_2: Byte = 1
    val b_1: Short = 1
    var b_2: Short = 1
    val c_1: Int = 1
    var c_2: Int = 1
    val d_1: Long = 1
    var d_2: Long = 1

    val e_1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    var e_2: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    val f_1: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    var f_2: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!>
    val g_1: Int = 1 + 2
    var g_2: Int = 1 + 2
    val h_1: Long = 1 + 2
    var h_2: Long = 1 + 2
}
