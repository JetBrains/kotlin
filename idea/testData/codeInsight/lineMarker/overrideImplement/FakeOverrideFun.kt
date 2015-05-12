interface <lineMarker></lineMarker>A {
  fun <lineMarker></lineMarker>f() {}
}

interface <lineMarker></lineMarker>B : A

interface <lineMarker></lineMarker>C : B, A

class SomeClass() : C {
  override fun <lineMarker descr="Overrides function in 'A'"></lineMarker>f() {}
}