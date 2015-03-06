class A {
  default object {
    fun Int.foo(a: Int = 1): Int {
        return a
    }

    fun test(): String {
        if (1.foo() != 1) return "fail"
        if (1.foo(2) != 2) return "fail"
        return "OK"
    }
  }
}

fun box(): String  {
   return A.test()
}