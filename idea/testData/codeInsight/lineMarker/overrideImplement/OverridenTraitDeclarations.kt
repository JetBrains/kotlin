// TODO: Declarations have no implementation and should be considered as "overloaded"
trait <lineMarker descr="*"></lineMarker>First {
    val <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;Second</body></html>"></lineMarker>some: Int
    var <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;Second</body></html>"></lineMarker>other: String
        get
        set

    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;Second</body></html>"></lineMarker>foo()
}

trait Second : First {
    override val <lineMarker descr="Overrides property in 'First'"></lineMarker>some: Int
    override var <lineMarker descr="Overrides property in 'First'"></lineMarker>other: String
    override fun <lineMarker descr="Overrides function in 'First'"></lineMarker>foo()
}