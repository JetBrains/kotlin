// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Int)

fun box() : String {
   val v1 = A(-10.toInt()).hashCode()
   val v2 = (-10.toInt() as Int?)!!.hashCode()
   return if( v1 == v2 ) "OK" else "$v1 $v2"
}
