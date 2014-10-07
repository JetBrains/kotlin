trait <lineMarker></lineMarker>A {
  fun <lineMarker></lineMarker>f() {}
}

trait <lineMarker></lineMarker>B : A

trait <lineMarker></lineMarker>C : B, A

class SomeClass() : C {
  override fun <lineMarker descr="Overrides function in 'A'"></lineMarker>f() {}
}