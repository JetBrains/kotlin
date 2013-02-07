data class A(val a: IntArray, var b: Array<String>)

fun box() : String {
   if( A(intArray(1,2,3),array("239")).hashCode() != 31*java.util.Arrays.hashCode(intArray(0,1,2)) + "239".hashCode()) "fail"
   return "OK"
}
