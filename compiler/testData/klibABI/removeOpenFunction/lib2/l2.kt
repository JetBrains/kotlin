class C2 : C() {
    override fun foo(): String = "O" // does not call super
}

class I2 : I {
    override fun foo(): String = "K" // does not call super
}
