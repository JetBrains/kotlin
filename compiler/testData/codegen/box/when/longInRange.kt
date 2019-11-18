// IGNORE_BACKEND_FIR: JVM_IR
class LongR {
  operator fun contains(l : Long): Boolean = l == 5.toLong()
}

fun box(): String {
  if (5 !in LongR()) return "fail 1"
  if (6 in LongR()) return "fail 2"
  return "OK"
}
