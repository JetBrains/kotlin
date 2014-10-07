trait <lineMarker></lineMarker>A {
    fun <lineMarker></lineMarker>f() {
    }
}

trait <lineMarker></lineMarker>B : A

open class <lineMarker></lineMarker>C(b : B) : B by b, A {
}

class D(b : B) : C(b) {
  override fun <lineMarker descr="Overrides function in 'A'"></lineMarker>f() {}
}