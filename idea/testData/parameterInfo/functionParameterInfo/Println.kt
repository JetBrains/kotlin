open class A(x: Int) {
    fun println(x: String) {}
    fun println() {}
    fun println(x: Boolean) {}
    
    fun d(x: Int) {
        println(<caret>)
    }
}
/*
Text: (<no parameters>), Disabled: false, Strikeout: false, Green: true
Text: (x: Boolean), Disabled: false, Strikeout: false, Green: false
Text: (x: String), Disabled: false, Strikeout: false, Green: false
*/