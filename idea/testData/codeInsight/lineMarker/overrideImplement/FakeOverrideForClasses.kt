package sample

interface <lineMarker descr="*">S</lineMarker><T> {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;sample.S2</body></html>">foo</lineMarker>(t: T): T

    val <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;sample.S2</body></html>">some</lineMarker>: T? get

    var <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;sample.S2</body></html>">other</lineMarker>: T?
        get
        set
}

open abstract class <lineMarker descr="*">S1</lineMarker> : S<String>

class S2 : S1() {
    override val <lineMarker descr="Implements property in 'S&lt;T&gt;'">some</lineMarker>: String = "S"

    override var <lineMarker descr="Implements property in 'S&lt;T&gt;'">other</lineMarker>: String?
        get() = null
        set(value) {}

    override fun <lineMarker descr="Implements function in 'S&lt;T&gt;'">foo</lineMarker>(t: String): String {
        return super<S1>.foo(t)
    }
}