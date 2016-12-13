interface <lineMarker descr="*">A</lineMarker> {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>">foo</lineMarker>(): String = "A"

    // TODO: B shoudn't be mentioned
    val <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;B<br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>">some</lineMarker>: String? get() = null

    // TODO: B shoudn't be mentioned
    var <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;B<br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>">other</lineMarker>: String?
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
