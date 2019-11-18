// IGNORE_BACKEND_FIR: JVM_IR
fun box () : String {
   val b = 4.toByte()
   val s = 5.toShort()
   val c: Char = 'A'
   return if( "$b" == "4" && " $b" == " 4" && "$s" == "5" && " $s" == " 5" && "$c" == "A" && " $c" == " A") "OK" else "fail"
}
