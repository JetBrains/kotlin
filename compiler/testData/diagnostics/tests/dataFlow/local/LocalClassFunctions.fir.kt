interface D {
  fun foo(): String = ""
}

fun test(d: Any?) {
  if (d !is D) return

  class Local {
    fun f() {
      d.foo()
    }

    fun f1() = d.foo()

    fun f2(): String = d.foo()
  }
}

