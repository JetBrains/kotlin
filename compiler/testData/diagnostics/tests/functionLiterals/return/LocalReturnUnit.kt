fun test(a: Int) {
    val x = run @f{
      if (a > 0) <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@f<!>
      else <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@f Unit<!>
    }
    x: Unit
}

fun run<T>(f: () -> T): T { return f() }
