// KJS_WITH_FULL_RUNTIME
fun isDigit(a: Int) : String {
    val aa = ArrayList<Int> ()
    aa.add(239)

    return when(a) {
      in aa -> "array list"
      in 0..9 -> "digit"
     !in 0..100 -> "not small"
      else -> "something"
    }
}

fun assertDigit(i: Int, expected: String): String {
  val result = isDigit(i)
  return if (result == expected) "" else "fail: isDigit($i) = \"$result\""
}

fun box(): String {
  val result =
  assertDigit(239, "array list") +
  assertDigit(0, "digit") +
  assertDigit(9, "digit") +
  assertDigit(5, "digit") +
  assertDigit(19, "something") +
  assertDigit(190, "not small")

  if (result == "") return "OK"
  return result
}
