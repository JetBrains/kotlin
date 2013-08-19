class Del {
  fun get(<!UNUSED_PARAMETER!>_this<!>: Any?, <!UNUSED_PARAMETER!>p<!>: PropertyMetadata): Int = 0
}

fun df(del: Del): Del = del


fun test(del: Any?) {
  if (del !is Del) return

  class Local {
    val delegatedVal by df(del)
    val delegatedVal1: Int by df(del)
  }
}

