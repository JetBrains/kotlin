// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Boolean)

fun box() : String {
   if (A(true).hashCode() != 1) return "fail1"
   if (A(false).hashCode() !=0) return "fail2"
   return "OK"
}
