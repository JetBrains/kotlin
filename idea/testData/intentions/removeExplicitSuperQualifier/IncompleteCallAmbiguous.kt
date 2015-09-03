// IS_APPLICABLE: false
// ERROR: <html>None of the following functions can be called with the arguments supplied. <ul><li>foo(<font color=red><b>Int</b></font>) <i>defined in</i> B</li><li>foo(<font color=red><b>String</b></font>) <i>defined in</i> B</li></ul></html>

open class B {
    open fun foo(p: String){}
    fun foo(p: Int){}
}

interface I {
    fun foo(p: String)
}

class A : B(), I {
    override fun foo(p: String) {
        super<B><caret>.foo()
    }
}