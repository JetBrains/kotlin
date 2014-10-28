open class A {
    public open fun foo(): String = ""
}

class B : A() {
    public override fun <caret>foo(): String {
        return "abc"
    }
}
