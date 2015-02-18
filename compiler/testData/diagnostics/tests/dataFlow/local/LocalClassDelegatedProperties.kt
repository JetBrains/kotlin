class Del {
  fun get(_this: Any?, p: PropertyMetadata): Int = 0
}

fun df(del: Del): Del = del


fun test(del: Any?) {
  if (del !is Del) return

  class Local {
    val delegatedVal by df(<!DEBUG_INFO_SMARTCAST!>del<!>)
    val delegatedVal1: Int by df(<!DEBUG_INFO_SMARTCAST!>del<!>)
  }
}

