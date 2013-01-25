class Test {
  private val Int.foo: String = "OK"
    get() {
        val a = $foo
        return "OK"
    }

  fun test(): String {
     return 1.foo
  }
}

fun box(): String {
   return Test().test()
}