fun test(a: Int) {
    (run @f{
      if (a > 0) return@f
      else return@f Unit.VALUE
    }): Unit
}

fun run<T>(f: () -> T): T { return f() }