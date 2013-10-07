trait <lineMarker></lineMarker>A {
    fun <lineMarker></lineMarker>f() {
    }
}

trait <lineMarker></lineMarker>B : A

open class <lineMarker></lineMarker>C(b : B) : B by b, A {
}

class D(b : B) : C(b) {
  override fun <lineMarker descr="<b>internal</b> <b>open</b> <b>fun</b> f(): jet.Unit <i>defined in</i> D<br/>overrides<br/><b>internal</b> <b>open</b> <b>fun</b> f(): jet.Unit <i>defined in</i> A"></lineMarker>f() {}
}