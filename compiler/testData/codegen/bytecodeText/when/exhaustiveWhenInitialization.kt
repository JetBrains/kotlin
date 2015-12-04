enum class A { V }

fun box(): String {
  val a: A = A.V
  val b: Boolean
  when (a) {
    A.V -> b = true
  }
  return if (b) "OK" else "FAIL"
}

// 0 TABLESWITCH
// 1 LOOKUPSWITCH
// 1 ATHROW