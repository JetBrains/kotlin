var Int.foo: String = "fail"
  set(str: String) {
      $foo = str
  }

fun box(): String {
  val i = 1
  i.foo = "OK"
  return i.foo
}