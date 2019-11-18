// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Char)

fun box() : String {
   val v1 = A('a').hashCode()
   val v2 = ('a' as Char?)!!.hashCode()
   return if( v1 == v2 ) "OK" else "$v1 $v2"
}
