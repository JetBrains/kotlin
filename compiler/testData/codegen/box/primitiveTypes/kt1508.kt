// IGNORE_BACKEND_FIR: JVM_IR
fun test( n : Number ) = n.toInt().toLong() + n.toLong()

fun box() : String {
  val n : Number = 10
  return if(test(n) == 20.toLong()) "OK" else "fail"
}
