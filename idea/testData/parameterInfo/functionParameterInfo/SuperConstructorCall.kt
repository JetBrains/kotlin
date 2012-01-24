open class A(x: Int) {
}

class B(): A(<caret>5) {
    fun m() {
        A(3)
    }
}
//Text: (x: Int), Disabled: false, Strikeout: false, Green: true