fun test() {
    (run @f{
      run @ff {
        return@ff "2"
      }
      return@f 1
    }): Int
}

fun run<T>(f: () -> T): T { return f() }