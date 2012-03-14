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
Text: (x: jet.Boolean), Disabled: false, Strikeout: false, Green: false
Text: (x: jet.String), Disabled: false, Strikeout: false, Green: false
*/