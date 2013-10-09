trait <lineMarker></lineMarker>A {
  fun <lineMarker></lineMarker>f() {}
}

trait <lineMarker></lineMarker>B : A

trait <lineMarker></lineMarker>C : B, A

class SomeClass() : C {
  override fun <lineMarker descr="<b>internal</b> <b>open</b> <b>fun</b> f(): jet.Unit <i>defined in</i> SomeClass<br/>overrides<br/><b>internal</b> <b>open</b> <b>fun</b> f(): jet.Unit <i>defined in</i> A"></lineMarker>f() {}
}