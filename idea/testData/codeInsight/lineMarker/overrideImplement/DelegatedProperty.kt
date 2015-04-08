trait <lineMarker></lineMarker>A {
    val <lineMarker></lineMarker>f: Int
        get() = 3
}

trait <lineMarker></lineMarker>B : A

open class <lineMarker></lineMarker>C(b : B) : B by b, A {
}

class D(b : B) : C(b) {
    override val <lineMarker descr="Overrides property in 'A'"></lineMarker>f: Int = 2
}