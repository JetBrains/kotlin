data class A(val a: Boolean)

fun box() : String {
   return if( A(true).hashCode()==1 && A(false).hashCode()==0 ) "OK" else "fail"
}
