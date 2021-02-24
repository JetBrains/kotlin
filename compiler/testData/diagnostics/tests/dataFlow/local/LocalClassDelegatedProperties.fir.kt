import kotlin.reflect.KProperty

class Del {
  operator fun getValue(_this: Any?, p: KProperty<*>): Int = 0
}

fun df(del: Del): Del = del


fun test(del: Any?) {
  if (del !is Del) return

  class Local {
    val delegatedVal by df(del)
    val delegatedVal1: Int by df(del)
  }
}
