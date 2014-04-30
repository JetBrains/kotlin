open class A() {
    open fun f() = 43
}

class B() : A() {
    override fun f() = super<caret>@B.f() - 1
}
