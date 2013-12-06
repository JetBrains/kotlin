open class Base(x: String, y: Int)

fun test(x: Any, y: Int?) {
  if (x !is String) return
  if (y == null) return

  class Local: Base(<!DEBUG_INFO_AUTOCAST!>x<!>, <!DEBUG_INFO_AUTOCAST!>y<!>) {
  }
}

