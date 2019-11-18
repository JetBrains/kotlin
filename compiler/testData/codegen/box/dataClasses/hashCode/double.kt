// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Double)

fun box() : String {
   val v1 = A(-10.toDouble()).hashCode()
   val v2 = (-10.toDouble() as Double?)!!.hashCode()
   return if( v1 == v2 ) "OK" else "$v1 $v2"
}
