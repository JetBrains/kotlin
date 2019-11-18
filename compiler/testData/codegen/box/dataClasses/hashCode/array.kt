// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

data class A(val a: IntArray, var b: Array<String>)

fun box() : String {
   if( A(intArrayOf(1,2,3),arrayOf("239")).hashCode() != 31*java.util.Arrays.hashCode(intArrayOf(0,1,2)) + "239".hashCode()) "fail"
   return "OK"
}
