package sample

trait <lineMarker descr="*"></lineMarker>S<T> {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;sample.S2</body></html>"></lineMarker>foo(t: T): T

    val <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;sample.S2</body></html>"></lineMarker>some: T? get

    var <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;sample.S2</body></html>"></lineMarker>other: T?
        get
        set
}

open abstract class <lineMarker descr="*"></lineMarker>S1 : S<String>

class S2 : S1() {
    override val <lineMarker descr="Implements property in 'S&lt;T&gt;'"></lineMarker>some: String = "S"

    override var <lineMarker descr="Implements property in 'S&lt;T&gt;'"></lineMarker>other: String?
        get() = null
        set(value) {}

    override fun <lineMarker descr="Implements function in 'S&lt;T&gt;'"></lineMarker>foo(t: String): String {
        return super<S1>.foo(t)
    }
}