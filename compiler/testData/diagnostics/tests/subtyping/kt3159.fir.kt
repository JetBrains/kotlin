interface Super {
    var v: CharSequence
    val v2: CharSequence
}

class Sub: Super {
    override var v: String = "fail"
    override val v2: String = "ok"
}