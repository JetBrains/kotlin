interface <lineMarker descr="*">A</lineMarker> {
    fun <lineMarker descr="<html><body>Is overridden in <br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>">foo</lineMarker>(): String = "A"

    val <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>">some</lineMarker>: String? get() = null

    var <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>">other</lineMarker>: String?
        get() = null
        set(value) {}
}

open class <lineMarker descr="*">B</lineMarker> : A

class C: B() {
    override val <lineMarker descr="Overrides property in 'A'">some</lineMarker>: String = "S"

    override var <lineMarker descr="Overrides property in 'A'">other</lineMarker>: String?
        get() = null
        set(value) {}

    override fun <lineMarker descr="Overrides function in 'A'">foo</lineMarker>(): String {
        return super<S1>.foo()
    }
}
