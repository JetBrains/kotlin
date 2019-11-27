// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Float)

fun box() : String {
   val v1 = A(-10.toFloat()).hashCode()
   val v2 = (-10.toFloat() as Float?)!!.hashCode()
   return if( v1 == v2 ) "OK" else "$v1 $v2"
}
