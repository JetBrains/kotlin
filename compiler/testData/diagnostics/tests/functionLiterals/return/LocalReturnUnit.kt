fun test(a: Int) {
    val x = run f@{
      if (a > 0) return@f
      else return@f Unit
    }
    x: Unit
}

fun run<T>(f: () -> T): T { return f() }