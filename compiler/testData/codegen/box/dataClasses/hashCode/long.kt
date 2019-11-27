// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Long)

fun box() : String {
   val v1 = A(-10.toLong()).hashCode()
   val v2 = (-10.toLong() as Long?)!!.hashCode()
   return if( v1 == v2 ) "OK" else "$v1 $v2"
}
