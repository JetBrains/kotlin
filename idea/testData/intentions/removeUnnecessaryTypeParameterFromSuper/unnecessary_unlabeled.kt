trait A {
    fun bar() {
    }
}

class C() : A {
    override fun bar() {
        super<<caret>A>.bar()
    }
}
