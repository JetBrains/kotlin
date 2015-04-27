fun test2(a: Int) {
    val x = run f@{
      if (a > 0) <!RETURN_NOT_ALLOWED!>return<!>
      return@f 1
    }
    x: Int
}

fun run<T>(f: () -> T): T { return f() }