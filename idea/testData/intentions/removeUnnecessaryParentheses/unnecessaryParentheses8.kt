class C() {
    fun get(i: Int) = i + 1

    // KT-4513: Unnecessary parenthesis around 'this'
    val foo = (th<caret>is)[41]
}
