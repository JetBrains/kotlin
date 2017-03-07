// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

data class A(val a: IntArray, var b: Array<String>)

fun box() : String {
   if( A(intArrayOf(1,2,3),arrayOf("239")).hashCode() != 31*java.util.Arrays.hashCode(intArrayOf(0,1,2)) + "239".hashCode()) "fail"
   return "OK"
}
