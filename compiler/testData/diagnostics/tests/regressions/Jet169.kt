fun set(<!UNUSED_PARAMETER!>key<!> : String, <!UNUSED_PARAMETER!>value<!> : String) {
  val a : String? = ""
  when (a) {
    "" -> a.get(0)
    is String, is Any -> a.compareTo("")
    else -> a.toString()
  }
}
