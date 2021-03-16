// FIR_IDENTICAL

fun set(<warning>key</warning> : String, <warning>value</warning> : String) {
  val a : String? = ""
  when (a) {
    "" -> a.get(0)
    is String, is Any -> a.compareTo("")
    else -> a.toString()
  }
}
