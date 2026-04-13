class F {
    var a = 1
    fun handleLeftBracketInFragment() {
        foo().<expr>peek()</expr>.a++
    }

    fun foo() : F = F()
    fun peek() : F = this
}
