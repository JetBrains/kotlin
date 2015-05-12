interface <lineMarker></lineMarker>A {
    val <lineMarker></lineMarker>f: Int
        get() = 2
}

interface <lineMarker></lineMarker>B : A

interface <lineMarker></lineMarker>C : B, A

class SomeClass() : C {
    override val <lineMarker descr="Overrides property in 'A'"></lineMarker>f: Int = 4
}