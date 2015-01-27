open class Base(<!UNUSED_PARAMETER!>x<!>: String, <!UNUSED_PARAMETER!>y<!>: Int)

fun test(x: Any, y: Int?) {
  if (x !is String) return
  if (y == null) return

  class Local: Base(<!DEBUG_INFO_SMARTCAST!>x<!>, <!DEBUG_INFO_SMARTCAST!>y<!>) {
  }
}

