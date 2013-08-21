fun test2(a: Int) {
    (run @f{
      if (a > 0) <!RETURN_NOT_ALLOWED!>return<!>
      <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@f 1<!>
    }): Int
}

fun run<T>(f: () -> T): T { return f() }