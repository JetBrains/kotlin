fun test() {
    (run @f{
      fun local(a: Int): String {
        if (a > 0) return "2"
        return@local "3"
      }
      return@f 1
    }): Int
}

fun run<T>(f: () -> T): T { return f() }