var Double.fooDouble: String = "fail"
  set(str: String) {
      $fooDouble = str
  }

var Long.fooLong: String = "fail"
  set(str: String) {
      $fooLong = str
  }

fun box(): String {
   val d = 1.0
   d.fooDouble = "O"
   val l = 1.toLong()
   l.fooLong = "K"
   return d.fooDouble + l.fooLong
}