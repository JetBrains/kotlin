open class <lineMarker></lineMarker>A {
  open fun <lineMarker></lineMarker>a(){
  }
}

open class <lineMarker descr="<html><body>Is subclassed by<br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"></lineMarker>B:A(){
  override fun <lineMarker
      descr="<b>internal</b> <b>open</b> <b>fun</b> a(): jet.Unit <i>defined in</i> B<br/>overrides<br/><b>internal</b> <b>open</b> <b>fun</b> a(): jet.Unit <i>defined in</i> A"></lineMarker><lineMarker
      descr="<html><body>Is overridden in <br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"></lineMarker>a(){
  }
}

class C:B(){
  override fun <lineMarker></lineMarker>a(){
  }
}