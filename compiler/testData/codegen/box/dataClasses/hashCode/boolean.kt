data class A(val a: Boolean)

fun box() : String {
   if (A(true).hashCode() != 1231) return "fail1"
   if (A(false).hashCode() != 1237) return "fail2"
   return "OK"
}
