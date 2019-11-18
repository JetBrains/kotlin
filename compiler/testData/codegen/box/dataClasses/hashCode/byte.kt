// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Byte)

fun box() : String {
   val v1 = A(10.toByte()).hashCode()
   val v2 = (10.toByte() as Byte?)!!.hashCode()
   return if( v1 == v2 ) "OK" else "$v1 $v2"
}
