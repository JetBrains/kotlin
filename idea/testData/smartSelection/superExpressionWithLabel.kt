open class EE() {
    open fun f() = 43
}

class FF() : EE() {
    override fun f() = <caret>super@FF.f() - 1
}
/*
super@FF.f()
super@FF.f() - 1
*/