open class A(x: Int) {
}

class B(): A(<caret>5) {
    fun m() {
        A(3)
    }
}
//Text: (<highlight>x: Int</highlight>), Disabled: false, Strikeout: false, Green: true