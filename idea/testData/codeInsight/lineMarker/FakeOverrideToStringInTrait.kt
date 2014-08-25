trait <lineMarker descr="*"></lineMarker>A {
    override fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"><lineMarker descr="*"></lineMarker></lineMarker>toString() = "A"
}

abstract class <lineMarker descr="*"></lineMarker>B: A

class C: B() {
    override fun <lineMarker descr="Overrides function in 'A'"></lineMarker>toString() = "B"
}
