trait Super {
    var v: CharSequence
    val v2: CharSequence
}

class Sub: Super {
    override var v: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>String<!> = "fail"
    override val v2: String = "ok"
}