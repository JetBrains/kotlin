fun test(a: Int) {
    val x = run @f{
      if (a > 0) <!RETURN_TYPE_MISMATCH!>return@f<!>
      else return@f Unit
    }
    x: Unit
}

fun run<T>(f: () -> T): T { return f() }