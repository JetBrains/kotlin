trait <lineMarker></lineMarker>A {
    val <lineMarker></lineMarker>f: Int
        get() = 3
}

trait <lineMarker></lineMarker>B : A

open class <lineMarker></lineMarker>C(b : B) : B by b, A {
}

class D(b : B) : C(b) {
    override val <lineMarker descr="<b>internal</b> <b>open</b> <b>val</b> f: jet.Int <i>defined in</i> D<br/>overrides<br/><b>internal</b> <b>open</b> <b>val</b> f: jet.Int <i>defined in</i> A"></lineMarker>f: Int = 2
}