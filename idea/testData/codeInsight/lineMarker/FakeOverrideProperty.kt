trait <lineMarker></lineMarker>A {
    val <lineMarker></lineMarker>f: Int
        get() = 2
}

trait <lineMarker></lineMarker>B : A

trait <lineMarker></lineMarker>C : B, A

class SomeClass() : C {
    override val <lineMarker descr="<b>internal</b> <b>open</b> <b>val</b> f: jet.Int <i>defined in</i> SomeClass<br/>overrides<br/><b>internal</b> <b>open</b> <b>val</b> f: jet.Int <i>defined in</i> A"></lineMarker>f: Int = 4
}