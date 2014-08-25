trait <lineMarker descr="*"></lineMarker>A {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;B</body></html>"></lineMarker>foo(str: String)
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;B</body></html>"></lineMarker>foo()
}

open class B : A {
    override fun <lineMarker descr="Implements function in 'A'"></lineMarker>foo(str: String) { }
    override fun <lineMarker descr="Implements function in 'A'"></lineMarker>foo() { }
}