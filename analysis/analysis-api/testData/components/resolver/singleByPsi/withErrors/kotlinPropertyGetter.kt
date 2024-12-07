fun call {
    val ktClass = KtClass()
    ktClass.<expr>foo</expr>
}

class KtClass {
    val foo: Int
        get() = 42
}
