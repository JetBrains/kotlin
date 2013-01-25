class Test {
  var Int.foo: String = "fail"
    set(str: String) {
        $foo = str
    }

  fun test(): String {
      val i = 1
      i.foo = "OK"
      return i.foo
  }
}

fun box(): String {
   return Test().test()
}