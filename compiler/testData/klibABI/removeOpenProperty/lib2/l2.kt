class C2 : C() {
    override val foo: String get() = "O" // does not call super
}

class I2 : I {
    override val foo: String get() = "K" // does not call super
}
