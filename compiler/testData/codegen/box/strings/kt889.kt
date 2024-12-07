operator fun Int.plus(s: String) : String {
  return s
}

fun box() : String {
   val s = "${1 + "a"}"
   return if(s == "a") "OK" else "fail"
}
