// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Any?, var x: Int)
data class B(val a: Any?)
data class C(val a: Int, var x: Int?)
data class D(val a: Int?)

fun box() : String {
   if( A(null,19).hashCode() != 19) "fail"
   if( A(239,19).hashCode() != (239*31+19)) "fail"
   if( B(null).hashCode() != 0) "fail"
   if( B(239).hashCode() != 239) "fail"
   if( C(239,19).hashCode() != (239*31+19)) "fail"
   if( C(239,null).hashCode() != 239*31) "fail"
   if( D(239).hashCode() != (239)) "fail"
   if( D(null).hashCode() != 0) "fail"
   return "OK"
}
