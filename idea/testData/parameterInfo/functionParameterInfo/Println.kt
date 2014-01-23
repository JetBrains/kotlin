open class A(x: Int) {
    fun println(x: String) {}
    fun println() {}
    fun println(x: Boolean) {}
    
    fun d(x: Int) {
        println(<caret>)
    }
}
/*
Text: (<highlight>x: Boolean</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>x: String</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<no parameters>), Disabled: true, Strikeout: false, Green: true
*/