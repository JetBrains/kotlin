trait <lineMarker descr="*"></lineMarker>A {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"></lineMarker>foo(): String = "A"

    // TODO: B shoudn't be mentioned
    val <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;B<br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"></lineMarker>some: String? get() = null

    // TODO: B shoudn't be mentioned
    var <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;B<br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"></lineMarker>other: String?
        get() = null
        set(value) {}
}

open class <lineMarker descr="*"></lineMarker>B: A

class C: B() {
    override val <lineMarker descr="Overrides property in 'A'"></lineMarker>some: String = "S"

    override var <lineMarker descr="Overrides property in 'A'"></lineMarker>other: String?
        get() = null
        set(value) {}

    override fun <lineMarker descr="Overrides function in 'A'"></lineMarker>foo(): String {
        return super<S1>.foo()
    }
}
