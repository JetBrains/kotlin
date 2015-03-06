fun set(key : String, value : String) {
  val a : String? = ""
  when (a) {
    "" -> a.get(0)
    is String, is Any -> a.compareTo("")
    else -> a.toString()
  }
}
