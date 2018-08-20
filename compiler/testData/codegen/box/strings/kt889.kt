// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

operator fun Int.plus(s: String) : String {
  System.out?.println("Int.plus(s: String) called")
  return s
}

fun box() : String {
   val s = "${1 + "a"}"
   return if(s == "a") "OK" else "fail"
}
