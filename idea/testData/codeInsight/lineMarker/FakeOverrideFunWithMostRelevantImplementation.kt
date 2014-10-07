trait <lineMarker></lineMarker>A {
    fun <lineMarker></lineMarker><lineMarker></lineMarker>f() {}
}

trait <lineMarker></lineMarker>B : A {
    override fun <lineMarker></lineMarker>f() {}
}

trait <lineMarker></lineMarker>C : B, A

class SomeClass() : C {
    override fun <lineMarker descr="Overrides function in 'B'"></lineMarker>f() {}
}