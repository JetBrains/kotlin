fun Int.plus(s: String) : String {
  System.out?.println("Int.plus(s: String) called")
  return s
}

fun box() : String {
   val s = "${1 + "a"}"
   return if(s == "a") "OK" else "fail"
}
