trait <lineMarker></lineMarker>A1 {
    fun <lineMarker></lineMarker>foo()
}

trait <lineMarker></lineMarker>B1 {
    fun <lineMarker></lineMarker>foo()
}

class C1: A1, B1 {
    override fun <lineMarker descr="Implements function in 'A1'<br/>Implements function in 'B1'"></lineMarker>foo() {}
}

/*
LINEMARKER: Implements function in 'A1'<br/>Implements function in 'B1'
TARGETS:
NavigateToSeveralSuperElements.kt
    fun <1>foo()
}

trait B1 {
    fun <2>foo()
*/