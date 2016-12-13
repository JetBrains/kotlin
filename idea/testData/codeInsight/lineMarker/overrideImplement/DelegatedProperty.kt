interface <lineMarker>A</lineMarker> {
    val <lineMarker>f</lineMarker>: Int
        get() = 3
}

interface <lineMarker>B</lineMarker> : A

open class <lineMarker>C</lineMarker>(b : B) : B by b, A {
}

class D(b : B) : C(b) {
    override val <lineMarker descr="Overrides property in 'A'">f</lineMarker>: Int = 2
}