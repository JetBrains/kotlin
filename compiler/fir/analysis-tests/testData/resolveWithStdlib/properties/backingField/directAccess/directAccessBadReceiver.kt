fun test() = "test"

fun box() {
    val a = <!SYNTHETIC_ACCESS_WRONG_RECEIVER!>10<!>#field
    val b = <!SYNTHETIC_ACCESS_WRONG_RECEIVER!>"test"<!>#self
    val c = (<!SYNTHETIC_ACCESS_WRONG_RECEIVER!>"a" + "b"<!>)#delegate
    val d = <!SYNTHETIC_ACCESS_WRONG_RECEIVER!>test()<!>#field

    with (20) {
        val e = <!SYNTHETIC_ACCESS_WRONG_RECEIVER!>this@with<!>#delegate
    }

    val g = <!SYNTHETIC_ACCESS_WRONG_RECEIVER!>::test<!>#field#self#self#delegate
}
