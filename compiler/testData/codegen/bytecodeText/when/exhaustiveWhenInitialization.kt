// IGNORE_BACKEND: JVM_IR
enum class A { V }

fun box(): String {
  val a: A = A.V
  val b: Boolean
  when (a) {
    A.V -> b = true
  }
  return if (b) "OK" else "FAIL"
}

// 1 TABLESWITCH
// 0 LOOKUPSWITCH
// 1 ATHROW
