// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
  val b = 1+1
  if ("$b" != "2") return "fail"
  if ("${1+1}" != "2") return "fail"
  return "OK"
}
