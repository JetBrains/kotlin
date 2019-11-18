// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Short)

fun box() : String {
   val v1 = A(10.toShort()).hashCode()
   val v2 = (10.toShort() as Short?)!!.hashCode()
   return if( v1 == v2 ) "OK" else "$v1 $v2"
}
