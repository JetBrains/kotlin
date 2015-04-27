fun test() {
    val x = run f@{
      run ff@ {
        return@ff "2"
      }
      return@f 1
    }
    x: Int
}

fun run<T>(f: () -> T): T { return f() }