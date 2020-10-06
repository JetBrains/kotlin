open class A(x: Int) {
    fun m(a: String = "a", b: String = "b", c: String = "c", d: String = "d") = 1

    fun d(x: Int) {
        m(b = "x", d = "x", <caret>)
    }
}
/*
Text: (<disabled>[b: String = "b"], [d: String = "d"],</disabled><highlight> </highlight>[a: String = "a"], [c: String = "c"]), Disabled: false, Strikeout: false, Green: true
*/