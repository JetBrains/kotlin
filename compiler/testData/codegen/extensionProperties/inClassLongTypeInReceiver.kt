class Test {
  var Double.fooDouble: String = "fail"
  var Long.fooLong: String = "fail"

  fun test(): String {
     val d = 1.0
     d.fooDouble = "O"
     val l = 1.toLong()
     l.fooLong = "K"
     return d.fooDouble + l.fooLong
  }
}

fun box(): String {
   return Test().test()
}