class C {
    fun foo(){}
    protected fun foo(p: Int){}
}

fun f(c: C) {
    c.foo(<caret>1)
}
// TODO: wrong name resolution. see: KT-11763
/*
Text: (<no parameters>), Disabled: true, Strikeout: false, Green: false
*/