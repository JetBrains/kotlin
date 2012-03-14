open class A(x: Int) {
}

class B(): A(5) {
    fun m() {
        A(<caret>3)
    }
}
//Text: (x: jet.Int), Disabled: false, Strikeout: false, Green: true