// WITH_STDLIB

import kotlin.test.*

fun if_else(b: Boolean): Int {
  if (b) return 42
  else   return 24
}

fun box(): String {
  val res = if_else(false)
  if (res != 24) return "FAIL: $res"

  return "OK"
}
