// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
 var a = 'a'

  if ("${a++}x" != "ax") return "fail1"

  if ("${a++}" != "b") return "fail2"

  return "OK"
}
