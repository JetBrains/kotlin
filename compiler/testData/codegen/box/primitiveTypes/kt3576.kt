object TestObject {
    val testFloat: Float = 0.9999
    val otherFloat: Float = 1.01
}

fun box(): String {
  return if (TestObject.testFloat.equals(0.9999.toFloat())
      && TestObject.otherFloat.equals(1.01.toFloat())) "OK" else "fail"
}