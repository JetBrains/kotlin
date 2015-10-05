class C {
    fun foo(){}
    protected fun foo(p: Int){}
}

fun f(c: C) {
    c.foo(<caret>1)
}
/*
Text: (<no parameters>), Disabled: false, Strikeout: false, Green: true
*/